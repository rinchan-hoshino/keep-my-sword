#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
git -C "${ROOT}" config core.hooksPath .githooks
chmod +x "${ROOT}"/.githooks/* "${ROOT}"/scripts/*.sh
echo "Configured core.hooksPath=$(git -C "${ROOT}" config --get core.hooksPath)"
