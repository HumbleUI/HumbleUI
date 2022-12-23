#! /usr/bin/env python3
import argparse, build_utils, functools, os, re

basedir = os.path.abspath(os.path.dirname(__file__) + '/..')
version = build_utils.get_arg("version") or build_utils.parse_ref() or build_utils.parse_sha() or "0.0.0-SNAPSHOT"
clojars = "https://repo.clojars.org"

@functools.lru_cache(maxsize=1)
def deps():
  deps = [
    build_utils.fetch_maven("org.clojure", "clojure", "1.11.1"),
    build_utils.fetch_maven("org.clojure", "core.specs.alpha", "0.2.62"),
    build_utils.fetch_maven("org.clojure", "spec.alpha", "0.3.218"),
    build_utils.fetch_maven("io.github.humbleui", "types", build_utils.deps_version("types$clojure"), classifier="clojure"),
    "src",
  ]

  parser = argparse.ArgumentParser()
  parser.add_argument('--jwm-dir', default=None)
  parser.add_argument('--jwm-version', default=build_utils.deps_version("jwm"))
  parser.add_argument('--skija-dir', default=None)
  parser.add_argument('--skija-version', default=build_utils.deps_version("skija-macos-arm64"))
  (args, _) = parser.parse_known_args()

  if args.jwm_dir:
    jwm_dir = args.jwm_dir
    if not os.path.isabs(jwm_dir):
      jwm_dir = build_utils.execdir + '/' + jwm_dir
    deps += [
      jwm_dir + '/target/classes',
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
