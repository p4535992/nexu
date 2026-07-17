#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
PROJECT_ROOT=$(cd -- "$SCRIPT_DIR/../../.." && pwd)
JAR_PATH=${1:-"$PROJECT_ROOT/nexu-app/target/nexu-app.jar"}
DESTINATION=${2:-"$PROJECT_ROOT/nexu-app/target/jpackage"}
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
PORTABLE_MARKER="$APP_IMAGE/.nexu-portable"
PORTABLE_CONTENTS="$DESTINATION/portable-contents.txt"

rm -rf "$DESTINATION"
mkdir -p "$INPUT_DIR"
cp "$JAR_PATH" "$INPUT_DIR/nexu-app.jar"

jpackage \
  --type app-image \
  --name "$APP_NAME" \
  --app-version "$APP_VERSION" \
  --vendor "NexU Community" \
  --description "Local smart-card signing agent" \
  --dest "$DESTINATION" \
  --input "$INPUT_DIR" \
  --main-jar nexu-app.jar \
  --add-modules "$MODULES" \
  --java-options '--add-exports=jdk.crypto.cryptoki/sun.security.pkcs11.wrapper=ALL-UNNAMED' \
  --java-options '--add-opens=jdk.crypto.cryptoki/sun.security.pkcs11=ALL-UNNAMED'

cp "$PROJECT_ROOT/LICENSE" "$APP_IMAGE/LICENSE"
cp "$PROJECT_ROOT/THIRD_PARTY_NOTICES.md" "$APP_IMAGE/THIRD_PARTY_NOTICES.md"
cp "$PROJECT_ROOT/nexu-app/src/main/resources/nexu-config.properties" \
  "$APP_IMAGE/nexu-config.properties"
cp "$SCRIPT_DIR/LOGS.txt" "$APP_IMAGE/LOGS.txt"
cp -R "$PROJECT_ROOT/licenses" "$APP_IMAGE/licenses"

# The marker is included only in the portable archive. At runtime it tells NexU
# to create ./logs beside the application image instead of using user data.
touch "$PORTABLE_MARKER"
tar -C "$DESTINATION" -czf "$ARCHIVE" "$APP_NAME"
tar -tzf "$ARCHIVE" > "$PORTABLE_CONTENTS"
grep -Fxq "$APP_NAME/.nexu-portable" "$PORTABLE_CONTENTS"
rm -f "$PORTABLE_CONTENTS"
rm -f "$PORTABLE_MARKER"
test ! -e "$PORTABLE_MARKER"

# Build an operator-friendly Debian package from the unmarked app image. The
# installed application therefore keeps using the user-writable data directory.
jpackage \
  --type deb \
  --name "$APP_NAME" \
  --app-version "$APP_VERSION" \
  --vendor "NexU Community" \
  --description "Local smart-card signing agent" \
  --dest "$DESTINATION" \
  --app-image "$APP_IMAGE" \
  --license-file "$PROJECT_ROOT/LICENSE" \
  --linux-package-name nexu \
  --linux-deb-maintainer "NexU Community" \
  --linux-menu-group "Utility" \
  --linux-app-category "Utility" \
  --linux-shortcut

rm -rf "$INPUT_DIR"

printf 'Application image: %s\nPortable archive: %s\n' "$APP_IMAGE" "$ARCHIVE"
find "$DESTINATION" -maxdepth 1 -type f -name '*.deb' -printf 'Debian installer: %p\n'
