#!/usr/bin/env python3
"""Nén toàn bộ ảnh của dự án mà KHÔNG làm giảm chất lượng hiển thị (nén không mất dữ liệu).

  • Icon launcher cũ (app/src/main/res/mipmap-*/ic_launcher*.png) → WebP lossless (điểm ảnh giữ nguyên 100%),
    nhỏ hơn khoảng 25% so với PNG. Android hỗ trợ WebP lossless từ API 18, app có minSdk 24.
  • Ảnh cửa hàng store/**.png (icon 512, feature graphic, ảnh chụp màn hình) → PNG tối ưu bằng oxipng (mức 6).
    Giữ PNG vì Google Play yêu cầu PNG/JPEG. Không lượng tử hóa màu vì gradient nền sẽ bị vỡ dải.

Mọi giao diện trong app (quân cờ, bàn cờ, biểu tượng) được vẽ bằng Canvas/vector nên không có ảnh bitmap nào khác.
Chạy:  pip install pillow pyoxipng && python3 tools/compress_images.py
"""
import glob
import io
import os

from PIL import Image

root = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
res = os.path.join(root, "app", "src", "main", "res")

try:
    import oxipng
except ImportError:  # dự phòng: PIL optimize (kém hơn một chút nhưng vẫn không mất dữ liệu)
    oxipng = None


def optimize_png(path: str) -> None:
    before = os.path.getsize(path)
    data = open(path, "rb").read()
    if oxipng is not None:
        out = oxipng.optimize_from_memory(data, level=6)
    else:
        buf = io.BytesIO(); Image.open(path).save(buf, "PNG", optimize=True); out = buf.getvalue()
    if len(out) < before:
        open(path, "wb").write(out)
    print(f"  {os.path.relpath(path, root)}: {before} -> {min(before, len(out))} B")


def png_to_webp(path: str) -> None:
    before = os.path.getsize(path)
    img = Image.open(path).convert("RGBA")
    out = path[:-4] + ".webp"
    img.save(out, "WEBP", lossless=True, quality=100, method=6, exact=True)
    os.remove(path)
    print(f"  {os.path.relpath(path, root)}: {before} -> {os.path.getsize(out)} B (webp lossless)")


def run() -> None:
    print("Icon launcher → WebP lossless")
    for p in sorted(glob.glob(os.path.join(res, "mipmap-*", "ic_launcher*.png"))):
        png_to_webp(p)
    print("Ảnh cửa hàng → PNG tối ưu")
    for p in sorted(glob.glob(os.path.join(root, "store", "**", "*.png"), recursive=True)):
        optimize_png(p)


if __name__ == "__main__":
    run()
