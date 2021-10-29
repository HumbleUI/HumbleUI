#! /usr/bin/env python3
import os, platform, subprocess, sys

def main():
  os.chdir(os.path.dirname(__file__) + '/..')
  detected_arch = {'AMD64': 'x64', 'x86_64': 'x64', 'arm64': 'arm64'}[platform.machine()]
  system = {'Darwin': 'macos', 'Linux': 'linux', 'Windows': 'windows'}[platform.system()]
  subprocess.check_call(["clojure",
    "-A:" + system + (("-" + detected_arch) if system == "macos" else "") + ":dev",
    "-M", "-m", "user",
    "--interactive"])
  return 0

if __name__ == '__main__':
  sys.exit(main())
