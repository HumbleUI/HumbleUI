#! /usr/bin/env python3
import build_utils, common, os, sys

def main():
  os.chdir(common.basedir)
  classpath = [
    build_utils.fetch_maven('org.projectlombok', 'lombok', '1.18.22'),
    build_utils.fetch_maven('org.jetbrains', 'annotations', '20.1.0'),
  ]
  build_utils.javac(build_utils.files("java/**/*.java"), "target/classes", classpath=classpath, modulepath=classpath)
  return 0

if __name__ == '__main__':
  sys.exit(main())
