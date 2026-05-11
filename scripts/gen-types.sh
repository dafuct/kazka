#!/usr/bin/env bash
# scripts/gen-types.sh — generate packages/shared/src/api-types.ts from
# the running backend's /v3/api-docs endpoint.
#
# Usage:
#   npm run gen:types                  # uses default URL http://localhost:8080
#   KAZKA_API=http://staging:8080 npm run gen:types
#
# Requires the backend to be running. Start it with:
#   cd backend && ./gradlew bootRun

set -euo pipefail

API_URL="${KAZKA_API:-http://localhost:8080}"
SPEC_URL="$API_URL/v3/api-docs"
OUT_PATH="packages/shared/src/api-types.ts"
TMP_PATH="$(mktemp -t kazka-openapi.XXXXXX.json)"

trap 'rm -f "$TMP_PATH"' EXIT

echo "Fetching OpenAPI spec from $SPEC_URL …"
if ! curl -fsS --max-time 10 "$SPEC_URL" -o "$TMP_PATH"; then
  echo "ERROR: failed to fetch $SPEC_URL — is the backend running?" >&2
  echo "Start it with: cd backend && ./gradlew bootRun" >&2
  exit 1
fi

echo "Generating $OUT_PATH …"
npx openapi-typescript "$TMP_PATH" \
  --output "$OUT_PATH" \
  --root-types

echo "Wrote $OUT_PATH"
