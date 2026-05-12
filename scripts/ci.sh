#!/usr/bin/env bash
set -euo pipefail

./gradlew --no-daemon build

if [[ "${RUN_SERVER_SMOKE:-1}" == "1" ]]; then
  ./scripts/server-smoke.sh
else
  echo "server smoke skipped: RUN_SERVER_SMOKE=$RUN_SERVER_SMOKE"
fi
