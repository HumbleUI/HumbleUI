#! /usr/bin/env python3
import argparse, build_utils, common, os, platform, subprocess, sys

def main():
  os.chdir(common.basedir)
  classpath = common.deps() + common.dev_deps()

  return subprocess.run(["java",
    "--class-path", build_utils.classpath_join(classpath),
    "-ea",
    "-Djdk.attach.allowAttachSelf",
    # "-XX:+UnlockDiagnosticVMOptions",
    # "-XX:+DebugNonSafepoints",
    "clojure.main", "--report", "stderr", "-m", "user"],
    # env = {"MTL_HUD_ENABLED": "1"}
    ).returncode

if __name__ == '__main__':
  sys.exit(main())
