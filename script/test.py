#! /usr/bin/env python3
import build_utils, common, os, platform, subprocess, sys

def main():
  os.chdir(common.basedir)
  classpath = common.deps() + common.dev_deps()

  return subprocess.call(["java",
    "--class-path", build_utils.classpath_join(classpath),
    "-ea",
    "clojure.main", "-e", "(require 'user)(user/-test-main nil)"])

if __name__ == '__main__':
  sys.exit(main())
