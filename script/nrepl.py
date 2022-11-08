#! /usr/bin/env python3
import build_utils, common, os, platform, subprocess, sys

def main():
  os.chdir(common.basedir)
  classpath = common.deps() + [
    "dev",
    build_utils.fetch_maven("nrepl", "nrepl", "1.0.0", repo = common.clojars),
    build_utils.fetch_maven("org.clojure", "tools.namespace", "1.3.0", repo = common.clojars),
    build_utils.fetch_maven("org.clojure", "java.classpath", "1.0.0", repo = common.clojars),
    build_utils.fetch_maven("org.clojure", "tools.reader", "1.3.6", repo = common.clojars)
  ]
  return subprocess.call(["java",
    "--class-path", build_utils.classpath_join(classpath),
    "clojure.main", "-m", "user", "--interactive"]
  )

if __name__ == '__main__':
  sys.exit(main())
