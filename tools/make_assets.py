#!/usr/bin/env python3
"""Vẽ icon (PNG cho Android cũ + 512px cho Play) và ảnh feature graphic 1024x500 bằng Pillow."""
import os
from PIL import Image, ImageDraw, ImageFont, ImageFilter
root = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
res = os.path.join(root, "app", "src", "main", "res")
SS = 4
TILES = [((27, 27), "#14937A"), ((55, 27), "#6A5FE0"), ((27, 55), "#E0A12A"), ((55, 55), "#D6477A")]

def vgrad(size, top, bottom):
    img = Image.new("RGB", (size, size)); px = img.load()
    t = tuple(int(top[i:i+2], 16) for i in (1, 3, 5)); b = tuple(int(bottom[i:i+2], 16) for i in (1, 3, 5))
    for y in range(size):
        k = y / (size - 1); c = tuple(int(t[j] + (b[j] - t[j]) * k) for j in range(3))
        for x in range(size): px[x, y] = c
    return img

def icon(size, shape="full"):
    n = size * SS; s = n / 108.0
    img = vgrad(n, "#1B2050", "#0A0D1D").convert("RGBA")
    glow = Image.new("RGBA", (n, n), (0, 0, 0, 0)); gd = ImageDraw.Draw(glow)
    gd.ellipse([n * .2, n * .2, n * .8, n * .8], fill=(240, 200, 100, 60)); glow = glow.filter(ImageFilter.GaussianBlur(n * .08))
    img = Image.alpha_composite(img, glow)
    d = ImageDraw.Draw(img)
    for (x, y), col in TILES:
        d.rounded_rectangle([x * s, y * s, (x + 26) * s, (y + 26) * s], radius=6 * s, fill=col)
    gem = [(42, 45), (48.5, 36), (59.5, 36), (66, 45), (54, 73)]
    gp = [(x * s, y * s) for x, y in gem]
    d.polygon(gp, fill="#F4D98A", outline="#0E1128", width=int(2.4 * s))
    for a, b in [((42, 45), (66, 45)), ((48.5, 36), (51.5, 45)), ((51.5, 45), (54, 73)), ((59.5, 36), (56.5, 45)), ((56.5, 45), (54, 73))]:
        d.line([a[0] * s, a[1] * s, b[0] * s, b[1] * s], fill="#0E1128", width=int(1.4 * s))
    img = img.resize((size, size), Image.LANCZOS)
    if shape != "full":
        mask = Image.new("L", (size * SS, size * SS), 0); md = ImageDraw.Draw(mask)
        if shape == "round": md.ellipse([0, 0, size * SS - 1, size * SS - 1], fill=255)
        else: md.rounded_rectangle([0, 0, size * SS - 1, size * SS - 1], radius=int(size * SS * .22), fill=255)
        img.putalpha(mask.resize((size, size), Image.LANCZOS))
    return img

for dens, px in {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}.items():
    out = os.path.join(res, f"mipmap-{dens}"); os.makedirs(out, exist_ok=True)
    icon(px, "rounded").save(os.path.join(out, "ic_launcher.png"))
    icon(px, "round").save(os.path.join(out, "ic_launcher_round.png"))
icon(512, "full").save(os.path.join(root, "store", "icon-512.png"))

# Feature graphic 1024x500
W, H = 1024, 500
bg = Image.new("RGB", (W, H)); px = bg.load()
for y in range(H):
    for x in range(W):
        k = (x / W) * .6 + (y / H) * .4
        px[x, y] = (int(20 + (8 - 20) * k), int(24 + (10 - 24) * k), int(58 + (26 - 58) * k))
bg = bg.convert("RGBA")
halo = Image.new("RGBA", (W, H), (0, 0, 0, 0)); ImageDraw.Draw(halo).ellipse([-80, 40, 520, 560], fill=(110, 100, 230, 60))
bg = Image.alpha_composite(bg, halo.filter(ImageFilter.GaussianBlur(70)))
ic = icon(300, "rounded"); bg.alpha_composite(ic, (80, 100))
d = ImageDraw.Draw(bg)
serif = "/usr/share/fonts/truetype/dejavu/DejaVuSerif-Bold.ttf"
sans = "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"
d.text((430, 110), "Play Mind", font=ImageFont.truetype(serif, 86), fill="#F2EFFA")
d.text((434, 214), "BOARD & BRAIN", font=ImageFont.truetype(sans, 34), fill="#E8C56E")
x = 432
for label, col in [("Puzzle", "#14937A"), ("Intellect", "#6A5FE0"), ("Calculation", "#E0A12A"), ("Memory", "#D6477A")]:
    f = ImageFont.truetype(sans, 19); w = d.textlength(label, font=f) + 30
    d.rounded_rectangle([x, 300, x + w, 346], radius=23, fill=col)
    d.text((x + 15, 312), label, font=f, fill="#FFFFFF"); x += w + 10
d.text((434, 380), "Chess  ·  Xiangqi  ·  Gomoku", font=ImageFont.truetype(sans, 26), fill="#A4A7CB")
bg.convert("RGB").save(os.path.join(root, "store", "feature-graphic-1024x500.png"))
print("assets ok")
