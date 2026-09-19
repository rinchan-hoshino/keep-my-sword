#!/usr/bin/env bash
set -euo pipefail
# A passing run must execute gameplay assertions, not just reach the EULA prompt.
exec python3 runtime-tests/run.py "$@"
