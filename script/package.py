#! /usr/bin/env python3
import argparse, build_utils, common, glob, os, re, subprocess, sys
from typing import List, Tuple

def main() -> Tuple[str, str, str]:
  os.chdir(common.basedir)

  build_utils.copy_replace(
    "deploy/META-INF/maven/io.github.humbleui/humbleui/pom.xml",
    "target/maven/META-INF/maven/io.github.humbleui/humbleui/pom.xml",
    {"${version}": common.version}
  )

  build_utils.copy_replace(
    "deploy/META-INF/maven/io.github.humbleui/humbleui/pom.properties",
    "target/maven/META-INF/maven/io.github.humbleui/humbleui/pom.properties",
    {"${version}": common.version}
  )
  
  jar = build_utils.jar(f"target/humbleui-{common.version}.jar",
    ("src", "."),
    ("target/maven", "META-INF")
  )

  return jar

if __name__ == "__main__":
  main()
  sys.exit(0)
