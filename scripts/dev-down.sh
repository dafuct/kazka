#!/usr/bin/env bash
# dev-down.sh — stop everything dev-up.sh started.
#
# Usage:
#   ./scripts/dev-down.sh             # stop processes + simulator app, keep docker
#   ./scripts/dev-down.sh --all       # also docker compose down
#   ./scripts/dev-down.sh --volumes   # also remove mysql data volume

set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PID_DIR="$ROOT/scripts/.pids"
STOP_DOCKER=0
WIPE_VOLUMES=0

for arg in "$@"; do
  case "$arg" in
    --all) STOP_DOCKER=1 ;;
    --volumes) STOP_DOCKER=1; WIPE_VOLUMES=1 ;;
    -h|--help)
      sed -n '2,8p' "$0"
      exit 0
      ;;
  esac
done

bold()  { printf "\033[1m%s\033[0m\n" "$*"; }
green() { printf "\033[32m%s\033[0m\n" "$*"; }
warn()  { printf "\033[33m%s\033[0m\n" "$*"; }

stop_pid() {
  local name="$1"
  local pidfile="$PID_DIR/$name.pid"
  if [ ! -f "$pidfile" ]; then
    warn "  $name: no pidfile (was it ever started?)"
    return 0
  fi
  local pid; pid="$(cat "$pidfile")"
  if kill -0 "$pid" 2>/dev/null; then
    # Kill the whole process group so child npm/gradle wrappers go too.
    kill -TERM "-$pid" 2>/dev/null || kill -TERM "$pid" 2>/dev/null || true
    sleep 2
    if kill -0 "$pid" 2>/dev/null; then
      kill -KILL "-$pid" 2>/dev/null || kill -KILL "$pid" 2>/dev/null || true
    fi
    green "  $name stopped (pid $pid)"
  else
    warn "  $name not running (stale pidfile)"
  fi
  rm -f "$pidfile"
}

bold "Stopping app processes"
stop_pid metro
stop_pid frontend
stop_pid backend

# Belt-and-braces: pkill anything still bound to the dev ports.
for pat in "expo start" "vite" "gradle.*bootRun"; do
  pkill -TERM -f "$pat" 2>/dev/null || true
done

bold "iOS Simulator"
if command -v xcrun >/dev/null && xcrun simctl list devices booted | grep -q "Booted"; then
  xcrun simctl terminate booted app.kazka.ios 2>/dev/null || true
  green "  Kazkar.app terminated (Simulator left running)"
else
  warn "  no booted Simulator"
fi

if [ "$STOP_DOCKER" -eq 1 ]; then
  bold "Docker compose"
  if [ "$WIPE_VOLUMES" -eq 1 ]; then
    ( cd "$ROOT" && docker compose down -v )
    warn "  mysql + redis stopped, volumes wiped"
  else
    ( cd "$ROOT" && docker compose down )
    green "  mysql + redis stopped"
  fi
else
  warn "Docker containers left running (use --all to stop them)"
fi
