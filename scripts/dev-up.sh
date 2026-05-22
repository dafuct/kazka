#!/usr/bin/env bash
# dev-up.sh — start the full local stack for Kazka:
#   1. MySQL + Redis (docker compose)
#   2. Backend (Spring Boot, port 8080)
#   3. Frontend (Vite, port 5173)
#   4. Mobile Metro bundler (port 8081)
#   5. iOS Simulator + Kazkar.app
#
# Usage:
#   ./scripts/dev-up.sh              # start everything, print summary, exit
#   ./scripts/dev-up.sh --no-mobile  # skip iOS bits (useful in headless setups)
#
# Logs land in scripts/.logs/*.log, PIDs in scripts/.pids/*.pid.
# Tear down with scripts/dev-down.sh.

set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
LOG_DIR="$ROOT/scripts/.logs"
PID_DIR="$ROOT/scripts/.pids"
SIM_DEVICE="${KAZKA_SIM_DEVICE:-iPhone 17 Pro}"
SKIP_MOBILE=0

for arg in "$@"; do
  case "$arg" in
    --no-mobile) SKIP_MOBILE=1 ;;
    -h|--help)
      sed -n '2,15p' "$0"
      exit 0
      ;;
  esac
done

mkdir -p "$LOG_DIR" "$PID_DIR"

# Load root .env so backend (Spring Boot) and Metro inherit ADMIN_*, OAuth,
# mail, and other shared secrets. Frontend reads it directly via Vite envDir.
# .env uses docker service names (e.g. DB_URL points at host `mysql`); when
# running on the host we remap those to localhost.
if [ -f "$ROOT/.env" ]; then
  set -a
  # shellcheck disable=SC1091
  source "$ROOT/.env"
  set +a
  if [ -n "${DB_URL:-}" ]; then
    _from='//mysql:'; _to='//localhost:'
    DB_URL="${DB_URL/${_from}/${_to}}"
    export DB_URL
    unset _from _to
  fi
fi

bold()  { printf "\033[1m%s\033[0m\n" "$*"; }
green() { printf "\033[32m%s\033[0m\n" "$*"; }
warn()  { printf "\033[33m%s\033[0m\n" "$*"; }
fail()  { printf "\033[31m%s\033[0m\n" "$*" >&2; exit 1; }

wait_for_http() {
  local url="$1" label="$2" max="${3:-60}"
  local i=0
  until curl -fs -o /dev/null "$url"; do
    i=$((i+1))
    if [ "$i" -ge "$max" ]; then
      fail "$label did not respond at $url after ${max}s"
    fi
    sleep 1
  done
  green "  $label up: $url"
}

start_bg() {
  # start_bg <name> <log> <cmd...>
  local name="$1" log="$2"; shift 2
  if [ -f "$PID_DIR/$name.pid" ] && kill -0 "$(cat "$PID_DIR/$name.pid")" 2>/dev/null; then
    warn "  $name already running (pid $(cat "$PID_DIR/$name.pid")) — skipping"
    return 0
  fi
  ( "$@" >"$log" 2>&1 & echo $! > "$PID_DIR/$name.pid" )
  green "  $name pid=$(cat "$PID_DIR/$name.pid")  log=$log"
}

# 1. Docker -------------------------------------------------------------
bold "[1/5] docker compose: mysql + redis"
command -v docker >/dev/null || fail "docker not on PATH"
( cd "$ROOT" && docker compose up -d mysql redis ) >/dev/null
until docker compose -f "$ROOT/docker-compose.yml" ps --format '{{.Service}} {{.Health}}' \
      | grep -q "^mysql healthy"; do
  sleep 2
done
green "  mysql + redis healthy"

# 2. Backend ------------------------------------------------------------
bold "[2/5] backend: spring boot (port 8080)"
start_bg backend "$LOG_DIR/backend.log" \
  bash -c "cd '$ROOT/backend' && ./gradlew bootRun"
wait_for_http http://localhost:8080/v3/api-docs backend 180

# 3. Frontend -----------------------------------------------------------
bold "[3/5] frontend: vite (port 5173)"
start_bg frontend "$LOG_DIR/frontend.log" \
  bash -c "cd '$ROOT/frontend' && npm run dev"
wait_for_http http://localhost:5173 frontend 60

# 4-5. Mobile -----------------------------------------------------------
if [ "$SKIP_MOBILE" -eq 1 ]; then
  warn "[4/5] mobile: skipped (--no-mobile)"
  warn "[5/5] simulator: skipped (--no-mobile)"
else
  bold "[4/5] mobile: metro bundler (port 8081)"
  start_bg metro "$LOG_DIR/metro.log" \
    bash -c "cd '$ROOT/mobile' && npx expo start --port 8081"
  wait_for_http http://localhost:8081/status metro 60

  bold "[5/5] iOS Simulator: $SIM_DEVICE"
  if ! command -v xcrun >/dev/null; then
    warn "  xcrun not on PATH — skipping simulator launch"
  else
    xcrun simctl boot "$SIM_DEVICE" 2>/dev/null || true
    open -a Simulator
    until xcrun simctl list devices booted | grep -q "$SIM_DEVICE"; do sleep 1; done
    if xcrun simctl launch booted app.kazka.ios >/dev/null 2>&1; then
      green "  Kazkar.app launched"
    else
      warn "  Kazkar.app not installed yet — run a fresh build:"
      warn "    cd mobile/ios && xcodebuild -workspace Kazkar.xcworkspace \\"
      warn "      -scheme Kazkar -configuration Debug \\"
      warn "      -destination 'platform=iOS Simulator,name=$SIM_DEVICE' \\"
      warn "      -derivedDataPath build build"
      warn "    xcrun simctl install booted mobile/ios/build/Build/Products/Debug-iphonesimulator/Kazkar.app"
      warn "    xcrun simctl launch booted app.kazka.ios"
    fi
  fi
fi

# Summary ---------------------------------------------------------------
echo
bold "Kazka dev stack up:"
echo "  backend   http://localhost:8080  (api docs: /v3/api-docs)"
echo "  frontend  http://localhost:5173"
if [ "$SKIP_MOBILE" -ne 1 ]; then
  echo "  metro     http://localhost:8081  (mobile bundler)"
fi
echo
echo "  logs: $LOG_DIR/*.log"
echo "  stop: $ROOT/scripts/dev-down.sh"
