#! /usr/bin/env python3
import os, platform, subprocess, sys, urllib.request

arch = {'AMD64': 'x64', 'x86_64': 'x64', 'arm64': 'arm64'}[platform.machine()]
system = {'Darwin': 'macos', 'Linux': 'linux', 'Windows': 'windows'}[platform.system()]
skija_artifact = "skija-" + system + (('-' + arch) if system == 'macos' else '')
classpath_separator = ';' if system == 'windows' else ':'
root = os.path.abspath(os.path.dirname(__file__) + '/..')
clojars = "https://repo.clojars.org"

def fetch(url, file):
  if not os.path.exists(file):
    print('Downloading', url)
    if os.path.dirname(file):
      os.makedirs(os.path.dirname(file), exist_ok = True)
    content = urllib.request.urlopen(url).read()
    with open(file, 'wb') as f:
      f.write(content)

def fetch_maven(group, name, version, classifier=None, repo='https://repo1.maven.org/maven2'):
  path = '/'.join([group.replace('.', '/'), name, version, name + '-' + version + ('-' + classifier if classifier else '') + '.jar'])
  file = os.path.join(os.path.expanduser('~'), '.m2', 'repository', path)
  fetch(repo + '/' + path, file)
  return file
