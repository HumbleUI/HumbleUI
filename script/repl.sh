#!/bin/bash
set -o errexit -o nounset -o pipefail
cd `dirname $0`/..

clj -M:dev -m user
