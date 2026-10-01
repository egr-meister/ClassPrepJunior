#!/usr/bin/env bash
# Fails if the packaged release APK requests any permission outside the allowlist.
# Usage: scripts/check_permissions.sh path/to/app-release.apk
set -euo pipefail
APK="$1"
AAPT2="$(ls -d "${ANDROID_HOME:-$ANDROID_SDK_ROOT}"/build-tools/*/ | sort -V | tail -1)aapt2"
ALLOWED=("android.permission.POST_NOTIFICATIONS" "android.permission.RECEIVE_BOOT_COMPLETED")
# AndroidX adds a signature-level DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION scoped to this app; it is
# not a user-facing permission and grants nothing to other apps, so it is allowed explicitly.
APP_ID="$("$AAPT2" dump packagename "$APK")"
ALLOWED+=("${APP_ID}.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION")

mapfile -t PERMS < <("$AAPT2" dump permissions "$APK" | sed -n "s/^uses-permission: name='\([^']*\)'.*/\1/p")
echo "Requested permissions:"; printf '  %s\n' "${PERMS[@]}"
status=0
for p in "${PERMS[@]}"; do
  ok=0
  for a in "${ALLOWED[@]}"; do [[ "$p" == "$a" ]] && ok=1; done
  if [[ $ok -eq 0 ]]; then echo "::error::Unexpected permission: $p"; status=1; fi
done
for forbidden in android.permission.INTERNET android.permission.ACCESS_NETWORK_STATE android.permission.SCHEDULE_EXACT_ALARM android.permission.USE_EXACT_ALARM android.permission.WAKE_LOCK android.permission.FOREGROUND_SERVICE; do
  if printf '%s\n' "${PERMS[@]}" | grep -qx "$forbidden"; then echo "::error::Forbidden permission present: $forbidden"; status=1; fi
done
exit $status
