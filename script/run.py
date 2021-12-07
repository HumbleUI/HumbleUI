#! /usr/bin/env python3
import common, os, platform, subprocess, sys

def main():
  os.chdir(common.root)

  classpath = [
    "src",
    common.fetch_maven("org.clojure", "clojure", "1.11.0-alpha3"),
    common.fetch_maven("org.clojure", "core.specs.alpha", "0.2.62"),
    common.fetch_maven("org.clojure", "spec.alpha", "0.2.194"),
    common.fetch_maven("io.github.humbleui.jwm", "jwm", "0.2.6"),
    common.fetch_maven("io.github.humbleui.skija", "skija-shared", "0.96.0"),
    common.fetch_maven("io.github.humbleui.skija", common.skija_artifact, "0.96.0"),
    "dev",
    common.fetch_maven("nrepl", "nrepl", "0.8.3", repo = common.clojars),
  ]
  return subprocess.call(["java",
    "--class-path", common.classpath_separator.join(classpath)]
    + (["-XstartOnFirstThread"] if common.system == 'macos' else []) +
    ["clojure.main",
     "-m", "user",
     "--interactive"]
  )

if __name__ == '__main__':
  sys.exit(main())
