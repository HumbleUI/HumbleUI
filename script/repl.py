#! /usr/bin/env python3
import argparse, build_utils, common, os, platform, subprocess, sys

def main():
  os.chdir(common.basedir)
  classpath = common.deps() + [
    "dev",
    "test",
    build_utils.fetch_github("tonsky", "tools.namespace", build_utils.deps_version("tools.namespace"), path='src/main/clojure'),
    build_utils.fetch_maven("org.clojure", "java.classpath", "1.0.0"),
    build_utils.fetch_maven("org.clojure", "tools.reader", "1.3.6"),

    build_utils.fetch_maven("criterium", "criterium", build_utils.deps_version("criterium"), repo = common.clojars),
    build_utils.fetch_maven("com.clojure-goes-fast", "clj-async-profiler", build_utils.deps_version("clj-async-profiler"), repo = common.clojars),
  ]
  
  parser = argparse.ArgumentParser()
  parser.add_argument('--ns', default='examples')
  (args, _) = parser.parse_known_args()

  return subprocess.call(["java",
    "--class-path", build_utils.classpath_join(classpath),
    "-ea",
    "-Djdk.attach.allowAttachSelf",
    # "-XX:+UnlockDiagnosticVMOptions",
    # "-XX:+DebugNonSafepoints",
    "clojure.main", "--report", "stderr", "-m", "user", "--ns", args.ns])

if __name__ == '__main__':
  sys.exit(main())
