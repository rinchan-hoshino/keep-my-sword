#!/usr/bin/env bash
set -euo pipefail

workdir="$(mktemp -d)"
trap 'rm -rf "$workdir"' EXIT
mkdir -p "$workdir/repo"
tar -xf - -C "$workdir/repo"
cd "$workdir/repo"

./scripts/ci.sh
