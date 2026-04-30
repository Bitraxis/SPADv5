set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
ROOT_DIR=$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd)

BUILD_DIR="$ROOT_DIR/build/tools"
CLASS_DIR="$BUILD_DIR/classes"
DIST_DIR="$ROOT_DIR/dist"
LIB_DIR="$DIST_DIR/lib/spad"
BIN_DIR="$DIST_DIR/bin"

rm -rf "$CLASS_DIR" "$DIST_DIR"
mkdir -p "$CLASS_DIR" "$LIB_DIR" "$BIN_DIR"

javac -d "$CLASS_DIR" "$ROOT_DIR"/main/*.java "$ROOT_DIR"/dragon/*.java
jar --create --file "$LIB_DIR/spad-tools.jar" -C "$CLASS_DIR" .

cp "$ROOT_DIR/scripts/launchers/dragon" "$BIN_DIR/dragon"
cp "$ROOT_DIR/scripts/launchers/spad" "$BIN_DIR/spad"
cp "$ROOT_DIR/scripts/launchers/dragon.cmd" "$BIN_DIR/dragon.cmd"
cp "$ROOT_DIR/scripts/launchers/spad.cmd" "$BIN_DIR/spad.cmd"
chmod +x "$BIN_DIR/dragon" "$BIN_DIR/spad"

echo "Build complete"
echo "- Jar: $LIB_DIR/spad-tools.jar"
echo "- Launchers: $BIN_DIR"
