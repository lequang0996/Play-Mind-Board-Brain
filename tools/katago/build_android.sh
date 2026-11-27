#!/usr/bin/env bash
# Biên dịch KataGo (backend Eigen, chạy CPU) thành tệp chạy cho Android và tải hai mạng g170 (CC0) vào assets.
# Kết quả (không commit, xem .gitignore):
#   app/src/main/jniLibs/<abi>/libkatago.so   (tệp chạy được, đặt tên lib*.so để Android cho chạy từ thư mục thư viện của app)
#   app/src/main/assets/katago/b6.bin.gz      (g170-b6c96, cho cấp Dễ / Trung bình / Khá)
#   app/src/main/assets/katago/b10.bin.gz     (g170e-b10c128, cho cấp Giỏi trở lên)
# Giấy phép: docs/licenses/KataGo-*.txt, Eigen-MPL2.txt. CI chạy script này (có cache) trước khi build app.
set -euo pipefail

KG_VER="1.18.2"
KG_SHA="fefd554bfaf0daa00d97ac3a767c7aef8d303545b135b0fbf8bd6837772f8bf9"
EIGEN_VER="3.4.0"
EIGEN_SHA="8586084f71f9bde545ee7fa6d00288b264a2b7ac3607b974e54d13e7162c1c72"
NET_B6="g170-b6c96-s175395328-d26788732.bin.gz"
NET_B6_SHA="f57fddf4672364d385d6ab177364ab819810d1123e229cb2649c4f337a2160b1"
NET_B10="g170e-b10c128-s1141046784-d204142634.bin.gz"
NET_B10_SHA="1a8e05a4ea3fca20dab79410cbb566c760767fcdd2fa0b701cfe259a84cc8b04"
ABIS="${KATAGO_ABIS:-arm64-v8a armeabi-v7a x86_64}"
API=24

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
WORK="${RUNNER_TEMP:-/tmp}/katago-build"
NDK="${ANDROID_NDK_LATEST_HOME:-${ANDROID_NDK_HOME:-${ANDROID_NDK_ROOT:-}}}"
[ -n "$NDK" ] && [ -d "$NDK" ] || { echo "::error::Không tìm thấy Android NDK (ANDROID_NDK_LATEST_HOME / ANDROID_NDK_HOME)"; exit 1; }
echo "NDK: $NDK"
mkdir -p "$WORK"; cd "$WORK"

fetch() {   # fetch <url> <file> <sha256>
  [ -f "$2" ] || curl -fsSL --retry 3 -o "$2" "$1"
  echo "$3  $2" | sha256sum -c - || { echo "::error::Sai mã băm $2"; rm -f "$2"; exit 1; }
}

fetch "https://github.com/lightvector/KataGo/archive/refs/tags/v$KG_VER.tar.gz" kg.tgz "$KG_SHA"
fetch "https://gitlab.com/libeigen/eigen/-/archive/$EIGEN_VER/eigen-$EIGEN_VER.tar.gz" eigen.tgz "$EIGEN_SHA"
rm -rf "KataGo-$KG_VER" "eigen-$EIGEN_VER"
tar xzf kg.tgz; tar xzf eigen.tgz
KG="$WORK/KataGo-$KG_VER/cpp"
# Android không có libatomic riêng (đã nằm trong runtime của NDK): bỏ dòng link "atomic".
sed -i 's/target_link_libraries(katago "atomic")/if(NOT ANDROID)\n      target_link_libraries(katago "atomic")\n    endif()/' "$KG/CMakeLists.txt"
# Một số file cần BYTE_ORDER; trên Android không header nào tự định nghĩa nó, nên chèn <endian.h> + dự phòng bằng macro của clang
# vào đầu từng file. Nếu bản KataGo mới thêm file dùng BYTE_ORDER thì script báo lỗi để cập nhật danh sách.
ENDIAN_FILES="core/sha2.cpp dataio/numpywrite.cpp neuralnet/desc.cpp"
FOUND="$(cd "$KG" && grep -rl 'BYTE_ORDER' --include='*.cpp' --include='*.h' . | sed 's|^\./||' | sort | xargs)"
WANT="$(printf '%s\n' $ENDIAN_FILES | sort | xargs)"
[ "$FOUND" = "$WANT" ] || { echo "::error::Danh sách file dùng BYTE_ORDER đã đổi: $FOUND"; exit 1; }
ENDIAN_HDR='#include <endian.h>
#ifndef BYTE_ORDER
#define LITTLE_ENDIAN 1234
#define BIG_ENDIAN 4321
#if __BYTE_ORDER__ == __ORDER_BIG_ENDIAN__
#define BYTE_ORDER BIG_ENDIAN
#else
#define BYTE_ORDER LITTLE_ENDIAN
#endif
#endif'
for f in $ENDIAN_FILES; do
  { printf '%s\n' "$ENDIAN_HDR"; cat "$KG/$f"; } > "$KG/$f.tmp" && mv "$KG/$f.tmp" "$KG/$f"
done

SYSROOT="$NDK/toolchains/llvm/prebuilt/linux-x86_64/sysroot"
for ABI in $ABIS; do
  case "$ABI" in
    arm64-v8a) TRIPLE=aarch64-linux-android ;;
    armeabi-v7a) TRIPLE=arm-linux-androideabi ;;
    x86_64) TRIPLE=x86_64-linux-android ;;
    *) echo "ABI lạ: $ABI"; exit 1 ;;
  esac
  B="$WORK/build-$ABI"
  cmake -S "$KG" -B "$B" \
    -DCMAKE_TOOLCHAIN_FILE="$NDK/build/cmake/android.toolchain.cmake" \
    -DANDROID_ABI="$ABI" -DANDROID_PLATFORM="android-$API" -DANDROID_STL=c++_static \
    -DCMAKE_BUILD_TYPE=Release -DUSE_BACKEND=EIGEN -DNO_GIT_REVISION=1 -DUSE_TCMALLOC=0 -DBUILD_DISTRIBUTED=0 \
    -DEIGEN3_INCLUDE_DIRS="$WORK/eigen-$EIGEN_VER" \
    -DZLIB_INCLUDE_DIR="$SYSROOT/usr/include" -DZLIB_LIBRARY="$SYSROOT/usr/lib/$TRIPLE/$API/libz.so"
  cmake --build "$B" --target katago -j"$(nproc)"
  mkdir -p "$ROOT/app/src/main/jniLibs/$ABI"
  cp "$B/katago" "$ROOT/app/src/main/jniLibs/$ABI/libkatago.so"
  "$NDK/toolchains/llvm/prebuilt/linux-x86_64/bin/llvm-strip" "$ROOT/app/src/main/jniLibs/$ABI/libkatago.so"
  ls -la "$ROOT/app/src/main/jniLibs/$ABI/libkatago.so"
done

mkdir -p "$ROOT/app/src/main/assets/katago"
fetch "https://katagoarchive.org/g170/neuralnets/$NET_B6" "$NET_B6" "$NET_B6_SHA"
fetch "https://katagoarchive.org/g170/neuralnets/$NET_B10" "$NET_B10" "$NET_B10_SHA"
cp "$NET_B6" "$ROOT/app/src/main/assets/katago/b6.bin.gz"
cp "$NET_B10" "$ROOT/app/src/main/assets/katago/b10.bin.gz"
echo "KataGo $KG_VER sẵn sàng cho: $ABIS"
