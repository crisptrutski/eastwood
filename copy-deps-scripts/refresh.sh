#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
classpath=$(clojure -Srepro -Spath -A:vendor)
python3 copy-deps-scripts/refresh.py "$classpath"
git apply copy-deps-scripts/patches/analyzer-jvm.patch
