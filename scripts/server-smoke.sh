#!/usr/bin/env bash
set -euo pipefail

smoke_one() {
  local task="$1"
  local log
  log="$(mktemp)"
  echo "server smoke: $task"
  set +e
  timeout --preserve-status 90s ./gradlew --no-daemon "$task" >"$log" 2>&1 &
  local pid=$!
  local ok=0
  for _ in $(seq 1 90); do
    if grep -Eq 'Done \([0-9.]+s\)! For help, type "help"|For help, type "help"|Starting minecraft server version|You need to agree to the EULA' "$log"; then
      ok=1
      break
    fi
    if ! kill -0 "$pid" 2>/dev/null; then
      break
    fi
    sleep 1
  done
  if kill -0 "$pid" 2>/dev/null; then
    kill "$pid" 2>/dev/null || true
    wait "$pid" 2>/dev/null || true
  else
    wait "$pid" >/dev/null 2>&1 || true
  fi
  set -e
  if [[ "$ok" == "1" ]]; then
    echo "server smoke passed: $task"
    rm -f "$log"
  else
    echo "server smoke failed: $task" >&2
    tail -120 "$log" >&2
    rm -f "$log"
    return 1
  fi
}

mapfile -t server_tasks < <(./gradlew --no-daemon tasks --all | awk '/:(runServer|server) / {print $1}' | sed 's/ .*//' | sort -u)
if ((${#server_tasks[@]} == 0)); then
  echo "server smoke skipped: no server run task found"
  exit 0
fi

for task in "${server_tasks[@]}"; do
  smoke_one "$task"
done
