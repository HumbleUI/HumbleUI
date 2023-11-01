#! /usr/bin/env python3
import argparse, base64, functools, glob, itertools, json, os, pathlib, platform, re, shutil, subprocess, tempfile, time, sys, urllib.request, zipfile
from typing import List, Tuple

def get_arg(name):
  parser = argparse.ArgumentParser()
  parser.add_argument(f'--{name}')
  (args, _) = parser.parse_known_args()
  return vars(args).get(name.replace("-", "_"))

execdir = os.getcwd()
native_arch = {'AMD64': 'x64', 'x86_64': 'x64', 'arm64': 'arm64', 'aarch64': 'arm64'}[platform.machine()]
arch   = get_arg("arch")   or native_arch
system = get_arg("system") or {'Darwin': 'macos', 'Linux': 'linux', 'Windows': 'windows'}[platform.system()]
classpath_separator = ';' if platform.system() == 'Windows' else ':'
mvn = "mvn.cmd" if platform.system() == "Windows" else "mvn"

def deps_version(name):
  with open("deps.edn") as f:
    for line in f.readlines():
      if m := re.search(re.escape(name) + '\\s*{\\s*:mvn/version\\s*"([0-9.]+)"', line):
        return m.group(1)

def classpath_join(entries):
  return classpath_separator.join(entries)

def parse_ref() -> str:
  ref = get_arg("ref") or os.getenv('GITHUB_REF')
  if ref and ref.startswith('refs/tags/'):
    return ref[len('refs/tags/'):]

def parse_sha() -> str:
  sha = get_arg("sha") or os.getenv('GITHUB_SHA')
  if sha:
    return sha[:10]

def makedirs(path):
  os.makedirs(path, exist_ok=True)

def rmdir(path):
  shutil.rmtree(path, ignore_errors=True)

def cat(iterables):
  return list(itertools.chain(*iterables))

def files(*patterns):
  return cat([glob.glob(pattern, recursive=True) for pattern in patterns])

def slurp(path):
  if os.path.exists(path):
    with open(path, 'r') as f:
      return f.read()

def copy_replace(src, dst, replacements):
  original = slurp(src)
  updated = original
  for key, value in replacements.items():
    updated = updated.replace(key, value)
  makedirs(os.path.dirname(dst))
  if updated != slurp(dst):
    print("Writing", dst, flush=True)
    with open(dst, 'w') as f:
      f.write(updated)

def copy_newer(src, dst):
  if not os.path.exists(dst) or os.path.getmtime(src) > os.path.getmtime(dst):
    if os.path.exists(dst):
      os.remove(dst)
    makedirs(os.path.dirname(dst))
    shutil.copy2(src, dst)
    return True

def has_newer(sources, targets):
  mtime = time.time()
  for target in targets:
    if os.path.exists(target):
      mtime = min(mtime, os.path.getmtime(target))
    else:
      mtime = 0
      break
  for path in sources:
    if os.path.getmtime(path) > mtime:
      return True
  return False

def fetch(url, file):
  if not os.path.exists(file):
    print('Downloading', url, flush=True)
    data = urllib.request.urlopen(url).read()
    if os.path.dirname(file):
      makedirs(os.path.dirname(file))
    with open(file, 'wb') as f:
      f.write(data)

def fetch_maven(group, name, version, classifier=None, repo='https://repo1.maven.org/maven2'):
  path = '/'.join([group.replace('.', '/'), name, version, name + '-' + version + ('-' + classifier if classifier else '') + '.jar'])
  file = os.path.join(os.path.expanduser('~'), '.m2', 'repository', path)
  fetch(repo + '/' + path, file)
  return file

def check_call(args):
  res = subprocess.call(args)
  if res != 0:
    cmd = ' '.join(args)
    if len(cmd) > 100:
      cmd = cmd[:100] + '...'
    print('---\nProcess "' + cmd + '" failed with code ' + str(res), flush=True)
    sys.exit(res)

def javac(sources, target, classpath = [], modulepath = [], add_modules = [], release = '11', opts=[]):
  makedirs(target)
  classes = {path.stem: path.stat().st_mtime for path in pathlib.Path(target).rglob('*.class') if '$' not in path.stem}
  newer = lambda path: path.stem not in classes or path.stat().st_mtime > classes.get(path.stem)
  new_sources = [path for path in sources if newer(pathlib.Path(path))]
  if new_sources:
    print('Compiling', len(new_sources), 'java files to', target + ':', new_sources, flush=True)
    check_call([
      'javac',
      '-encoding', 'UTF8',
      *opts,
      '--release', release,
      *(['--class-path', classpath_join(classpath + [target])] if classpath else []),
      *(['--module-path', classpath_join(modulepath)] if modulepath else []),
      *(['--add-modules', ','.join(add_modules)] if add_modules else []),
      '-d', target,
      *new_sources])

def jar(target: str, *content: List[Tuple[str, str]], opts=[]) -> str:
  if has_newer(files(*[dir + "/" + subdir + "/**" for (dir, subdir) in content]), [target]):
    print(f"Packaging {os.path.basename(target)}", flush=True)
    makedirs(os.path.dirname(target))
    check_call(["jar",
      "--create",
      "--file", target,
      *cat([["-C", dir, file] for (dir, file) in content])] + opts)
  return target

@functools.lru_cache(maxsize=1)
def lombok():
  return fetch_maven('org.projectlombok', 'lombok', '1.18.28')

def delombok(dirs: List[str], target: str, classpath: List[str] = [], modulepath: List[str] = []):
  sources = files(*[dir + "/**/*.java" for dir in dirs])
  if has_newer(sources, files(target + "/**")):
    print("Delomboking", *dirs, "to", target, flush=True)
    check_call(["java",
      "-Dfile.encoding=UTF8",
      "-jar", lombok(),
      "delombok",
      *dirs,
      '--encoding', 'UTF-8',
      *(["--classpath", classpath_join(classpath)] if classpath else []),
      *(["--module-path", classpath_join(modulepath)] if modulepath else []),
      "--target", target
    ])

def javadoc(dirs: List[str], target: str, classpath: List[str] = [], modulepath: List[str] = []):
  sources = files(*[dir + "/**/*.java" for dir in dirs])
  if has_newer(sources, files(target + "/**")):
    print("Generating JavaDoc", *dirs, "to", target, flush=True)
    check_call(["javadoc",
      *(["--class-path", classpath_join(classpath)] if classpath else []),
      *(["--module-path", classpath_join(modulepath)] if modulepath else []),
      "-d", target,
      "-quiet",
      "-Xdoclint:all,-missing",
      *sources])

def deploy(jar,
           tempdir = tempfile.gettempdir(),
           classifier = None,
           ossrh_username = os.getenv('OSSRH_USERNAME'),
           ossrh_password = os.getenv('OSSRH_PASSWORD'),
           repo="https://s01.oss.sonatype.org/service/local/staging"):
  makedirs(tempdir)
  settings = tempdir + "/settings.xml"
  with open(settings, 'w') as f:
    f.write("""
      <settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
            xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
            xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0
                                http://maven.apache.org/xsd/settings-1.0.0.xsd">
          <servers>
              <server>
                  <id>ossrh</id>
                  <username>${ossrh.username}</username>
                  <password>${ossrh.password}</password>
              </server>
          </servers>
      </settings>
    """)

  mvn_settings = [
    '--settings', settings,
    '-Dossrh.username=' + ossrh_username,
    '-Dossrh.password=' + ossrh_password,
    '-Durl=' + repo + "/deploy/maven2/",
    '-DrepositoryId=ossrh'
  ]

  with zipfile.ZipFile(jar, 'r') as f:
    pom = [path for path in f.namelist() if re.fullmatch(r"META-INF/maven/.*/pom\.xml", path)][0]
    f.extract(pom, tempdir)

  classifier = classifier or (re.fullmatch(r".*-\d+\.\d+\.\d+(?:-SNAPSHOT)?(?:-([a-z0-9\-]+))?\.jar", os.path.basename(jar))[1])

  print(f'Deploying {jar}', classifier, pom, flush=True)
  check_call(
    [mvn, 'gpg:sign-and-deploy-file'] + \
    mvn_settings + \
    [f'-DpomFile={tempdir}/{pom}',
     f'-Dfile={jar}']
    + ([f"-Dclassifier={classifier}"] if classifier else []))
  return 0

def release(ossrh_username = os.getenv('OSSRH_USERNAME'),
            ossrh_password = os.getenv('OSSRH_PASSWORD'),
            repo="https://s01.oss.sonatype.org/service/local/staging"):
  headers = {
    'Accept': 'application/json',
    'Authorization': 'Basic ' + base64.b64encode((ossrh_username + ":" + ossrh_password).encode('utf-8')).decode('utf-8'),
    'Content-Type': 'application/json',
  }

  def fetch(path, data = None):
    req = urllib.request.Request(repo + path,
                                 headers=headers,
                                 data = json.dumps(data).encode('utf-8') if data else None)
    resp = urllib.request.urlopen(req).read().decode('utf-8')
    print(' ', path, "->", resp, flush=True)
    return json.loads(resp) if resp else None

  print('Finding staging repo', flush=True)
  resp = fetch('/profile_repositories')
  if len(resp['data']) != 1:
    print("Too many open repositories:", [repo['repositoryId'] for repo in resp['data']], flush=True)
    return 1

  repo_id = resp['data'][0]["repositoryId"]
  
  print('Closing repo', repo_id, flush=True)
  resp = fetch('/bulk/close', data = {"data": {"description": "", "stagedRepositoryIds": [repo_id]}})

  while True:
    print('Checking repo', repo_id, 'status', flush=True)
    resp = fetch('/repository/' + repo_id + '/activity')
    close_events = [e for e in resp if e['name'] == 'close' and 'stopped' in e and 'events' in e]
    close_events = close_events[0]['events'] if close_events else []
    fail_events = [e for e in close_events if e['name'] == 'ruleFailed']
    if fail_events:
      print(fail_events, flush=True)
      return 1

    if close_events and close_events[-1]['name'] == 'repositoryClosed':
      break

    time.sleep(0.5)

  print('Releasing staging repo', repo_id, flush=True)
  resp = fetch('/bulk/promote', data = {"data": {
              "autoDropAfterRelease": True,
              "description": "",
              "stagedRepositoryIds":[repo_id]
        }})
  print('Success! Just released', repo_id, flush=True)
  return 0
