#! /usr/bin/env python3
import argparse, build_utils, common, os, platform, subprocess, sys

def main():
  os.chdir(common.basedir)
  classpath = common.deps() + [
    "dev",
    "test",
    build_utils.fetch_maven("org.clojure", "tools.namespace", build_utils.deps_version("tools.namespace")),
    build_utils.fetch_maven("org.clojure", "java.classpath", "1.0.0"),
    build_utils.fetch_maven("org.clojure", "tools.reader", "1.3.6"),
    build_utils.fetch_maven("criterium", "criterium", build_utils.deps_version("criterium"), repo = common.clojars)
  ]
  
  parser = argparse.ArgumentParser()
  parser.add_argument('--main', default='examples')
  (args, _) = parser.parse_known_args()

  return subprocess.call(["java",
    "--class-path", build_utils.classpath_join(classpath),
    "-ea",
    "clojure.main", "-m", args.main])

if __name__ == '__main__':
  sys.exit(main())
