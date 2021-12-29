#! /usr/bin/env python3
import argparse, build_utils, functools, os

basedir = os.path.abspath(os.path.dirname(__file__) + '/..')

clojars = "https://repo.clojars.org"

@functools.lru_cache(maxsize=1)
def deps_clojure():
  return [
    build_utils.fetch_maven("org.clojure", "clojure", "1.11.0-alpha3"),
    build_utils.fetch_maven("org.clojure", "core.specs.alpha", "0.2.62"),
    build_utils.fetch_maven("org.clojure", "spec.alpha", "0.2.194")
  ]

@functools.lru_cache(maxsize=1)
def deps_compile():
  return deps_clojure() + [
    build_utils.fetch_maven('org.projectlombok', 'lombok', '1.18.22'),
    build_utils.fetch_maven('org.jetbrains', 'annotations', '20.1.0'),
  ]

@functools.lru_cache(maxsize=1)
def deps_run():
  parser = argparse.ArgumentParser()
  parser.add_argument('--jwm-dir', default=None)
  parser.add_argument('--jwm-version', default="0.3.0")
  parser.add_argument('--skija-dir', default=None)
  parser.add_argument('--skija-version', default='0.98.0')
  (args, _) = parser.parse_known_args()

  deps = deps_clojure()
  deps += [
    "src",
    "target/classes",
  ]

  if args.jwm_dir:
    deps += [
      build_utils.execdir + '/' + args.jwm_dir + '/windows/build',
      build_utils.execdir + '/' + args.jwm_dir + '/linux/build',
      build_utils.execdir + '/' + args.jwm_dir + '/macos/build',
      build_utils.execdir + '/' + args.jwm_dir + '/target/classes',
    ]
  else:
    deps += [
      build_utils.fetch_maven('io.github.humbleui', 'jwm', args.jwm_version),
    ]

  if args.skija_dir:
    deps += [
      build_utils.execdir + '/' + args.skija_dir + '/platform/build',
      build_utils.execdir + '/' + args.skija_dir + '/platform/target/classes',
      build_utils.execdir + '/' + args.skija_dir + '/shared/target/classes',
    ]
  else:
    skija_native = 'skija-' + build_utils.system + (('-' + build_utils.arch) if 'macos' == build_utils.system else '')
    deps += [
      build_utils.fetch_maven('io.github.humbleui', 'skija-shared', args.skija_version),
      build_utils.fetch_maven('io.github.humbleui', skija_native, args.skija_version),
    ]

  return deps
