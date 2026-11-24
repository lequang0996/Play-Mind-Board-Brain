"""Quân cờ tướng (đĩa kính trong, như ảnh) và quân caro (X gỗ sẫm, O vòng gỗ sáng, như ảnh)."""
from dsl import *
import math

def ring_path(cx, cy, r, ccw=False):
    sw = 0 if ccw else 1
    return (f"M{f(cx-r)} {f(cy)}A{f(r)} {f(r)} 0 1 {sw} {f(cx+r)} {f(cy)}A{f(r)} {f(r)} 0 1 {sw} {f(cx-r)} {f(cy)}Z")

def arc(cx, cy, r, a0, a1):
    p0 = (cx + r * math.cos(math.radians(a0)), cy + r * math.sin(math.radians(a0)))
    p1 = (cx + r * math.cos(math.radians(a1)), cy + r * math.sin(math.radians(a1)))
    large = 1 if (a1 - a0) % 360 > 180 else 0
    return f"M{f(p0[0])} {f(p0[1])}A{f(r)} {f(r)} 0 {large} 1 {f(p1[0])} {f(p1[1])}"

# ───────────── Cờ tướng: đĩa kính ─────────────
def glass_disc(red):
    R = 46
    if red:
        body = [(0, '#FFE3E6/0.50'), (.55, '#F58C97/0.60'), (.86, '#D8334A/0.80'), (1, '#97132B/0.95')]
        ink = '#B3122B'
    else:
        body = [(0, '#FFFFFF/0.30'), (.55, '#8A8A99/0.50'), (.86, '#25252F/0.82'), (1, '#07070B/0.97')]
        ink = '#0C0C12'
    L = []
    L.append(fill(ring_path(50, 50, R), Rad(40, 38, 70, body)))
    # mặt vát (bevel) sáng phía trên-trái, tối phía dưới-phải
    L.append(stroke(arc(50, 50, R - 2.2, 190, 300), '#FFFFFF/0.85', 2.6))
    L.append(stroke(arc(50, 50, R - 2.2, 20, 110), '#000000/0.20' if not red else '#6A0A1C/0.35', 2.6))
    L.append(stroke(ring_path(50, 50, R - 0.6), '#000000/0.28' if not red else '#6A0A1C/0.55', 1.2))
    # bậc trong
    L.append(stroke(ring_path(50, 50, 37), '#FFFFFF/0.40', 1.4))
    L.append(stroke(arc(50, 50, 35.6, 20, 110), '#000000/0.18' if not red else '#6A0A1C/0.25', 1.2))
    # vòng khắc
    L.append(stroke(ring_path(50, 50, 33.2), ink + '/0.85', 2.0))
    # phản chiếu
    L.append(fill(ell(40, 27, 22, 9), Lin(0, 18, 0, 36, [(0, '#FFFFFF/0.65'), (1, '#FFFFFF/0.0')])))
    L.append(fill(ell(63, 75, 15, 5), Lin(0, 70, 0, 80, [(0, '#FFFFFF/0.0'), (1, '#FFFFFF/0.25')])))
    return L

XQ_RED = glass_disc(True)
XQ_BLACK = glass_disc(False)

# ───────────── Caro: X gỗ sẫm ─────────────
def bar(cx, cy, length, thick, ang, r=3.2):
    """Thanh chữ nhật bo góc xoay [ang] độ quanh (cx, cy) -> path (Q bo góc)."""
    a = math.radians(ang); c, s = math.cos(a), math.sin(a)
    hl, ht = length / 2, thick / 2
    def P(x, y): return (cx + x * c - y * s, cy + x * s + y * c)
    pts = [(-hl, -ht), (hl, -ht), (hl, ht), (-hl, ht)]
    d = ''
    for i in range(4):
        p0 = pts[i - 1]; p1 = pts[i]; p2 = pts[(i + 1) % 4]
        def toward(a_, b_, dist):
            dx, dy = b_[0] - a_[0], b_[1] - a_[1]; n = math.hypot(dx, dy); return (a_[0] + dx / n * dist, a_[1] + dy / n * dist)
        s0 = P(*toward(p1, p0, r)); s1 = P(*toward(p1, p2, r)); pc = P(*p1)
        d += ('M' if i == 0 else 'L') + f"{f(s0[0])} {f(s0[1])}Q{f(pc[0])} {f(pc[1])} {f(s1[0])} {f(s1[1])}"
    return d + 'Z'

def shift(d, dx, dy):
    import re
    out = []
    for cmd, args in re.findall(r'([MLQ])([^MLQZ]*)', d):
        n = [float(v) for v in re.findall(r'-?\d*\.?\d+', args)]
        for i in range(0, len(n), 2): n[i] += dx; n[i + 1] += dy
        out.append(cmd + ' '.join(f(v) for v in n))
    return ''.join(out) + 'Z'

def caro_x():
    b1 = bar(50, 50, 74, 21, 45); b2 = bar(50, 50, 74, 21, -45)
    L = []
    for b in (b1, b2): L.append(fill(shift(b, 1.6, 3.0), '#150C0B'))                      # độ dày (mặt bên)
    for b in (b1, b2): L.append(fill(b, Lin(18, 18, 82, 82, [(0, '#6A524C'), (.45, '#473430'), (1, '#2B1E1D')])))
    # vân gỗ
    for b, ang in ((b1, 45), (b2, -45)):
        g = []
        a = math.radians(ang); c, s = math.cos(a), math.sin(a)
        for k in (-6.5, -2.2, 2.4, 6.8):
            p0 = (50 + (-40) * c - k * s, 50 + (-40) * s + k * c); p1 = (50 + 40 * c - k * s, 50 + 40 * s + k * c)
            g.append(stroke(f"M{f(p0[0])} {f(p0[1])}L{f(p1[0])} {f(p1[1])}", '#FFFFFF/0.07', 0.8))
        L.append(clip(b, g))
    for b in (b1, b2): L.append(stroke(b, '#FFFFFF/0.10', 0.9))
    # viền sáng cạnh trên-trái
    L.append(stroke("M21 31L31 21", '#FFFFFF/0.18', 1.0))
    return L

def caro_o():
    Ro, Ri = 34, 20
    ring = ring_path(50, 50, Ro) + ring_path(50, 50, Ri, ccw=True)
    side = ring_path(51.4, 53.2, Ro) + ring_path(51.4, 53.2, Ri, ccw=True)
    L = [fill(side, '#A98C5C'),
         fill(ring, Lin(18, 18, 82, 82, [(0, '#FFFBF0'), (.5, '#F1E3C6'), (1, '#DCC699')])),
         stroke(ring_path(50, 50, Ro - 0.4), '#8A6B3C/0.55', 1.1),
         stroke(ring_path(50, 50, Ri + 0.4), '#8A6B3C/0.65', 1.2),
         stroke(arc(50, 50, 27.5, 195, 285), '#FFFFFF/0.7', 1.4)]
    g = []
    for k in (-22, -12, -3, 6, 16, 24):
        g.append(stroke(f"M{f(18)} {f(50+k)}Q{f(50)} {f(50+k+3)} {f(82)} {f(50+k-1)}", '#8A6B3C/0.13', 0.8))
    L.append(clip(ring, g))
    return L
