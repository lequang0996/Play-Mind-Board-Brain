#!/usr/bin/env bash
# Biên dịch KataGo (backend Eigen, CPU, AVX2) cho Linux x86-64 và tải hai mạng g170: CHỈ dùng trên CI để đo bot cờ vây
# (bot-bench, lệnh "ladder go"). Cùng phiên bản KataGo và cùng mạng với bản Android (build_android.sh).
# Kết quả: $OUT/katago, $OUT/b6.bin.gz, $OUT/b10.bin.gz (mặc định OUT=kgbin).
set -euo pipefail
OUT="$(realpath -m "${1:-kgbin}")"
KG_VER="1.18.2"
KG_SHA="fefd554bfaf0daa00d97ac3a767c7aef8d303545b135b0fbf8bd6837772f8bf9"
NET_B6="g170-b6c96-s175395328-d26788732.bin.gz"
NET_B6_SHA="f57fddf4672364d385d6ab177364ab819810d1123e229cb2649c4f337a2160b1"
NET_B10="g170e-b10c128-s1141046784-d204142634.bin.gz"
NET_B10_SHA="1a8e05a4ea3fca20dab79410cbb566c760767fcdd2fa0b701cfe259a84cc8b04"
WORK="${RUNNER_TEMP:-/tmp}/katago-linux"
mkdir -p "$WORK" "$OUT"; cd "$WORK"
fetch() {   # fetch <url> <file> <sha256>
  [ -f "$2" ] || curl -fsSL --retry 3 -o "$2" "$1"
  echo "$3  $2" | sha256sum -c - || { echo "::error::Sai mã băm $2"; rm -f "$2"; exit 1; }
}
sudo apt-get install -y -qq libeigen3-dev zlib1g-dev >/dev/null
fetch "https://github.com/lightvector/KataGo/archive/refs/tags/v$KG_VER.tar.gz" kg.tgz "$KG_SHA"
rm -rf "KataGo-$KG_VER"; tar xzf kg.tgz
cmake -S "KataGo-$KG_VER/cpp" -B build -DCMAKE_BUILD_TYPE=Release -DUSE_BACKEND=EIGEN -DUSE_AVX2=1 \
  -DNO_GIT_REVISION=1 -DUSE_TCMALLOC=0 -DBUILD_DISTRIBUTED=0 >/dev/null
cmake --build build --target katago -j"$(nproc)" >/dev/null
cp build/katago "$OUT/katago"
fetch "https://katagoarchive.org/g170/neuralnets/$NET_B6" "$NET_B6" "$NET_B6_SHA"
fetch "https://katagoarchive.org/g170/neuralnets/$NET_B10" "$NET_B10" "$NET_B10_SHA"
cp "$NET_B6" "$OUT/b6.bin.gz"; cp "$NET_B10" "$OUT/b10.bin.gz"
ls -la "$OUT"
