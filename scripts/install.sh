#!/usr/bin/env sh
set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
ROOT_DIR=$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd)
PREFIX="$HOME/.local"
ADD_PATH=0

while [ $# -gt 0 ]; do
  case "$1" in
    --add-path)
      ADD_PATH=1
      ;;
    --prefix)
      shift
      if [ $# -eq 0 ]; then
        echo "Missing value for --prefix" >&2
        exit 1
      fi
      PREFIX=$1
      ;;
    --help|-h)
      echo "Usage: $0 [--add-path] [--prefix <dir>]"
      exit 0
      ;;
    *)
      PREFIX=$1
      ;;
  esac
  shift
done

if [ ! -f "$ROOT_DIR/dist/lib/spad/spad-tools.jar" ]; then
  "$ROOT_DIR/scripts/build-all.sh"
fi

mkdir -p "$PREFIX/bin" "$PREFIX/lib/spad"
cp "$ROOT_DIR/dist/lib/spad/spad-tools.jar" "$PREFIX/lib/spad/spad-tools.jar"
cp "$ROOT_DIR/dist/bin/dragon" "$PREFIX/bin/dragon"
cp "$ROOT_DIR/dist/bin/spad" "$PREFIX/bin/spad"
chmod +x "$PREFIX/bin/dragon" "$PREFIX/bin/spad"

if [ "$ADD_PATH" -eq 1 ]; then
  PATH_LINE="export PATH=\"$PREFIX/bin:\$PATH\""
  PROFILE_FILE="$HOME/.profile"

  if [ ! -f "$PROFILE_FILE" ]; then
    : > "$PROFILE_FILE"
  fi

  if ! grep -Fqs "$PREFIX/bin" "$PROFILE_FILE"; then
    printf '\n# SPAD/Dragon tools\n%s\n' "$PATH_LINE" >> "$PROFILE_FILE"
  fi

  case ":$PATH:" in
    *":$PREFIX/bin:"*) ;;
    *) PATH="$PREFIX/bin:$PATH" ; export PATH ;;
  esac
fi

echo "Installed SPAD/Dragon tools to $PREFIX"
echo "If needed, add this to PATH: $PREFIX/bin"
if [ "$ADD_PATH" -eq 1 ]; then
  echo "Updated $HOME/.profile and current PATH when possible"
fi
echo "Try: dragon help"
