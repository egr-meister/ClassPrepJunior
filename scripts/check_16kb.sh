#!/usr/bin/env bash
# 16 KB page-size check: lists native libraries in the APK/AAB and, if any exist, checks ELF LOAD
# segment alignment (>= 2**14) and zip alignment of uncompressed .so files.
# Usage: scripts/check_16kb.sh app-release.apk app-release.aab
set -euo pipefail
APK="$1"; AAB="${2:-}"
BT="$(ls -d "${ANDROID_HOME:-$ANDROID_SDK_ROOT}"/build-tools/*/ | sort -V | tail -1)"
status=0
check_archive() {
  local f="$1"
  local libs
  libs="$(unzip -Z1 "$f" | grep -E '\.so$' || true)"
  if [[ -z "$libs" ]]; then
    echo "$f: no native .so libraries (16 KB ELF alignment not applicable)."
    return
  fi
  echo "$f: native libraries found:"; echo "$libs"
  local tmp; tmp="$(mktemp -d)"
  unzip -q "$f" '*.so' -d "$tmp"
  while IFS= read -r so; do
    local align
    align="$(objdump -p "$tmp/$so" | awk '/LOAD/ {print $NF}' | sort -u | head -1)"
    echo "  $so LOAD align=$align"
    if [[ "$align" != "2**14" && "$align" != "2**16" ]]; then echo "::error::$so is not 16 KB aligned ($align)"; status=1; fi
  done <<< "$libs"
  rm -rf "$tmp"
}
check_archive "$APK"
[[ -n "$AAB" ]] && check_archive "$AAB"
if unzip -Z1 "$APK" | grep -qE '\.so$'; then
  "$BT/zipalign" -c -P 16 -v 4 "$APK" | tail -1 || status=1
fi
exit $status
