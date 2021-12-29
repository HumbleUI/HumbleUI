#! /usr/bin/env python3
import build_utils, common, os, sys

def main():
  os.chdir(common.basedir)
  classpath = common.deps_compile()
  build_utils.javac(build_utils.files("java/**/*.java"), "target/classes", classpath=classpath, modulepath=classpath)
  return 0

if __name__ == '__main__':
  sys.exit(main())
