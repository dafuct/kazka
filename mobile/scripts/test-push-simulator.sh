#!/usr/bin/env bash
# Test the in-app notification handler against the iOS Simulator.
#
# Usage:
#   bash mobile/scripts/test-push-simulator.sh <storyId>
#
# Requires: iOS Simulator booted, Kazkar app installed via `expo run:ios`.

set -euo pipefail

STORY_ID="${1:-test-story-id-123}"
BUNDLE_ID="app.kazka.ios"

PAYLOAD=$(mktemp -t kazka-apns.XXXXXX.json)
cat > "$PAYLOAD" <<JSON
{
  "Simulator Target Bundle": "$BUNDLE_ID",
  "aps": {
    "alert": {
      "title": "Казка готова!",
      "body": "Тестова казка"
    },
    "sound": "default"
  },
  "type": "story_ready",
  "storyId": "$STORY_ID"
}
JSON

xcrun simctl push booted "$BUNDLE_ID" "$PAYLOAD"
echo "Pushed to booted Simulator for bundle $BUNDLE_ID with storyId=$STORY_ID"
rm -f "$PAYLOAD"
