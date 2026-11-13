#!/usr/bin/env bash
# Stops the apps started by run-local.sh
cd "$(dirname "$0")/.."
for f in logs-local/*.pid; do
  [ -f "$f" ] || continue
  kill "$(cat "$f")" 2>/dev/null && echo "stopped $(basename "$f" .pid)"
  rm -f "$f"
done
