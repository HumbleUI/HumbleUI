#! /usr/bin/env python3
import build_utils, common, os, platform, subprocess, sys, test

def main():
  os.chdir(common.basedir)
  classpath = common.deps() + [
    "dev",
    build_utils.fetch_maven("nrepl", "nrepl", "0.9.0", repo = common.clojars),
  ]
  return subprocess.call(["java",
    "--class-path", build_utils.classpath_join(classpath)]
    + (["-XstartOnFirstThread"] if build_utils.system == 'macos' else []) +
    ["clojure.main",
     "-m", "user",
     "--interactive"]
  )

if __name__ == '__main__':
  sys.exit(main())
