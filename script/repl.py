#! /usr/bin/env python3
import argparse, build_utils, common, os, platform, subprocess, sys

def main():
  os.chdir(common.basedir)
  classpath = common.deps() + common.dev_deps()

  return subprocess.run(["java",
    "--class-path", build_utils.classpath_join(classpath),
    "-ea",
    "-Duser.language=en",
    "-Duser.country=US",
    "-Dfile.encoding=UTF-8",
    
    # clj-async-profiler
    "-Djdk.attach.allowAttachSelf",
    "-XX:+UnlockDiagnosticVMOptions",
    "-XX:+DebugNonSafepoints",
    "-XX:+EnableDynamicAgentLoading",
    "-Dclj-async-profiler.output-dir=/ws/humbleui",

    # env = {"MTL_HUD_ENABLED": "1"},

    "clojure.main", "--report", "stderr", "-m", "user"],
    ).returncode

if __name__ == '__main__':
  sys.exit(main())
