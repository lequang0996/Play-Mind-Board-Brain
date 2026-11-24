#!/usr/bin/env bash
# Chụp ảnh cửa hàng từ ScreenshotActivity (bản debug). Chạy trong emulator (xem .github/workflows/screenshots.yml).
set -euo pipefail
PKG=com.fws.playmind.debug
OUT=store/screenshots
mkdir -p "$OUT"
adb install -r app/build/outputs/apk/debug/app-debug.apk
# Chế độ demo của thanh trạng thái cho ảnh gọn gàng
adb shell settings put global sysui_demo_allowed 1 || true
adb shell am broadcast -a com.android.systemui.demo -e command enter || true
adb shell am broadcast -a com.android.systemui.demo -e command clock -e hhmm 1000 || true
adb shell am broadcast -a com.android.systemui.demo -e command battery -e level 100 -e plugged false || true
adb shell am broadcast -a com.android.systemui.demo -e command notifications -e visible false || true

shot() { # tên  scene  lang
  adb shell am force-stop "$PKG"
  adb shell am start -n "$PKG/com.fws.playmind.ScreenshotActivity" --es scene "$2" --es lang "$3" >/dev/null
  sleep 4
  adb exec-out screencap -p > "$OUT/$1.png"
}

n=1
for lang in en vi; do
  for scene in home lobby chess xiangqi caro settings; do
    shot "$(printf '%02d' $n)_${lang}_${scene}" "$scene" "$lang"
    n=$((n+1))
  done
done
adb shell am broadcast -a com.android.systemui.demo -e command exit || true
ls -la "$OUT"
