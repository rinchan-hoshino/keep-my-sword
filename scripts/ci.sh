#!/usr/bin/env bash
set -euo pipefail
./gradlew --no-daemon :fabric:build :neoforge:build "$@"
