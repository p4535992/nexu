#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
PROJECT_ROOT=$(cd -- "$SCRIPT_DIR/../../.." && pwd)
JAR_PATH=${1:-"$PROJECT_ROOT/nexu-modern-app/target/nexu-modern.jar"}
DESTINATION=${2:-"$PROJECT_ROOT/nexu-modern-app/target/jpackage"}
APP_VERSION=${3:-"1.24.0"}
APP_NAME=NexU
MODULES=$(tr -d '\r\n' < "$SCRIPT_DIR/modules.txt")

if [[ ! -f "$JAR_PATH" ]]; then
  echo "Executable JAR not found: $JAR_PATH" >&2
  exit 1
fi

INPUT_DIR="$DESTINATION/input"
APP_IMAGE="$DESTINATION/$APP_NAME"
ARCHIVE="$DESTINATION/nexu-${APP_VERSION}-linux-$(uname -m)-portable.tar.gz"

rm -rf "$DESTINATION"
mkdir -p "$INPUT_DIR"
cp "$JAR_PATH" "$INPUT_DIR/nexu-modern.jar"

jpackage \
  --type app-image \
  --name "$APP_NAME" \
  --app-version "$APP_VERSION" \
  --vendor "NexU Community" \
  --description "Local smart-card signing agent" \
  --dest "$DESTINATION" \
  --input "$INPUT_DIR" \
  --main-jar nexu-modern.jar \
  --add-modules "$MODULES" \
  --java-options '--add-exports=jdk.crypto.cryptoki/sun.security.pkcs11.wrapper=ALL-UNNAMED' \
  --java-options '--add-opens=jdk.crypto.cryptoki/sun.security.pkcs11=ALL-UNNAMED'

cp "$PROJECT_ROOT/LICENSE" "$APP_IMAGE/LICENSE"
cp "$PROJECT_ROOT/THIRD_PARTY_NOTICES.md" "$APP_IMAGE/THIRD_PARTY_NOTICES.md"
cp "$PROJECT_ROOT/nexu-modern-app/src/main/resources/nexu-config.properties" \
  "$APP_IMAGE/nexu-config.properties"
cp -R "$PROJECT_ROOT/licenses" "$APP_IMAGE/licenses"

rm -rf "$INPUT_DIR"
tar -C "$DESTINATION" -czf "$ARCHIVE" "$APP_NAME"

printf 'Application image: %s\nPortable archive: %s\n' "$APP_IMAGE" "$ARCHIVE"
