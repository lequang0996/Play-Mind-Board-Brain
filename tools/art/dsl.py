"""
Mini-DSL mô tả hình vẽ (path + gradient + clip) trong hệ tọa độ 100x100.
Từ MỘT mô tả, xuất ra: (1) SVG để xem thử bằng mắt, (2) mã Kotlin Compose (Layer) dùng trong app.
Nhờ vậy bản xem thử và bản trong app luôn khớp nhau.

Màu: '#RRGGBB' hoặc '#RRGGBB/0.55' (kèm độ trong suốt) hoặc '$vaiTro' (lấy từ bảng màu của từng bên, vd '$bHi').
"""
import re

# ───────────── Paint ─────────────
class Lin:
    def __init__(self, x1, y1, x2, y2, stops): self.a = (x1, y1, x2, y2); self.stops = stops
class Rad:
    def __init__(self, cx, cy, r, stops): self.c = (cx, cy, r); self.stops = stops
class Sweep:
    def __init__(self, cx, cy, stops): self.c = (cx, cy); self.stops = stops

# ───────────── Layer ─────────────
def fill(d, paint): return ('fill', d, paint)
def stroke(d, color, w): return ('stroke', d, color, w)
def clip(d, layers): return ('clip', d, layers)

# ───────────── Hình học ─────────────
def f(v):
    s = ('%.2f' % v).rstrip('0').rstrip('.')
    return '0' if s in ('-0', '') else s

def ell(cx, cy, rx, ry):
    return f"M{f(cx-rx)} {f(cy)}A{f(rx)} {f(ry)} 0 1 0 {f(cx+rx)} {f(cy)}A{f(rx)} {f(ry)} 0 1 0 {f(cx-rx)} {f(cy)}Z"

def rect(x, y, w, h, r=0):
    if r <= 0: return f"M{f(x)} {f(y)}H{f(x+w)}V{f(y+h)}H{f(x)}Z"
    return (f"M{f(x+r)} {f(y)}H{f(x+w-r)}Q{f(x+w)} {f(y)} {f(x+w)} {f(y+r)}V{f(y+h-r)}Q{f(x+w)} {f(y+h)} {f(x+w-r)} {f(y+h)}"
            f"H{f(x+r)}Q{f(x)} {f(y+h)} {f(x)} {f(y+h-r)}V{f(y+r)}Q{f(x)} {f(y)} {f(x+r)} {f(y)}Z")

def poly(pts, close=True):
    return 'M' + 'L'.join(f"{f(x)} {f(y)}" for x, y in pts) + ('Z' if close else '')

def mirror_x(d, cx=50.0):
    """Lật đối xứng qua x=cx (chỉ hỗ trợ lệnh tuyệt đối M L C Q Z)."""
    out = []
    for cmd, args in re.findall(r'([MLCQZ])([^MLCQZ]*)', d):
        nums = [float(n) for n in re.findall(r'-?\d*\.?\d+', args)]
        for i in range(0, len(nums), 2): nums[i] = 2 * cx - nums[i]
        out.append(cmd + ' '.join(f(n) for n in nums))
    return ''.join(out)

def lathe(right_segs, top, bottom, cx=50.0):
    """
    Thân tròn xoay đối xứng. right_segs: danh sách đoạn nửa PHẢI đi từ trên xuống dưới, mỗi đoạn là
    ('L', x, y) | ('Q', cx, cy, x, y) | ('C', x1, y1, x2, y2, x, y). top/bottom: tọa độ y của tâm đỉnh/đáy.
    """
    d = f"M{f(cx)} {f(top)}"
    pts = [(cx, top)]
    for s in right_segs:
        d += s[0] + ' '.join(f(v) for v in s[1:])
        pts.append((s[-2], s[-1]))
    d += f"L{f(cx)} {f(bottom)}"
    # nửa trái đi ngược lên
    segs = list(right_segs)
    for i in range(len(segs) - 1, -1, -1):
        s = segs[i]; start = pts[i]
        if s[0] == 'L': d += f"L{f(2*cx-start[0])} {f(start[1])}"
        elif s[0] == 'Q': d += f"Q{f(2*cx-s[1])} {f(s[2])} {f(2*cx-start[0])} {f(start[1])}"
        else: d += f"C{f(2*cx-s[3])} {f(s[4])} {f(2*cx-s[1])} {f(s[2])} {f(2*cx-start[0])} {f(start[1])}"
    return d + 'Z'


# ───────────── Xuất SVG ─────────────
def _resolve(c, pal):
    if c.startswith('$'):
        name, _, a = c[1:].partition('/')
        base = pal[name]
        return base + (('/' + a) if a else '')
    return c

def _rgba(c):
    col, _, a = c.partition('/')
    return col, (float(a) if a else 1.0)

class Svg:
    def __init__(self): self.defs = []; self.n = 0
    def uid(self): self.n += 1; return f"g{self.n}"

    def paint(self, p, pal, uid_prefix):
        if isinstance(p, str):
            col, a = _rgba(_resolve(p, pal)); return col, a
        gid = f"{uid_prefix}{self.uid()}"
        def stops(st):
            s = ''
            for o, c in st:
                col, a = _rgba(_resolve(c, pal)); s += f'<stop offset="{o}" stop-color="{col}" stop-opacity="{a}"/>'
            return s
        if isinstance(p, Lin):
            x1, y1, x2, y2 = p.a
            self.defs.append(f'<linearGradient id="{gid}" gradientUnits="userSpaceOnUse" x1="{x1}" y1="{y1}" x2="{x2}" y2="{y2}">{stops(p.stops)}</linearGradient>')
        elif isinstance(p, Rad):
            cx, cy, r = p.c
            self.defs.append(f'<radialGradient id="{gid}" gradientUnits="userSpaceOnUse" cx="{cx}" cy="{cy}" r="{r}">{stops(p.stops)}</radialGradient>')
        return f'url(#{gid})', 1.0

    def render(self, layers, pal, pfx='p'):
        out = []
        for L in layers:
            if L[0] == 'fill':
                col, a = self.paint(L[2], pal, pfx)
                out.append(f'<path d="{L[1]}" fill="{col}" fill-opacity="{a}"/>')
            elif L[0] == 'stroke':
                col, a = _rgba(_resolve(L[2], pal))
                out.append(f'<path d="{L[1]}" fill="none" stroke="{col}" stroke-opacity="{a}" stroke-width="{L[3]}" stroke-linecap="round" stroke-linejoin="round"/>')
            elif L[0] == 'clip':
                cid = f"{pfx}c{self.uid()}"
                self.defs.append(f'<clipPath id="{cid}"><path d="{L[1]}"/></clipPath>')
                out.append(f'<g clip-path="url(#{cid})">' + self.render(L[2], pal, pfx) + '</g>')
        return ''.join(out)


# ───────────── Xuất Kotlin ─────────────
def _kcolor(c):
    col, a = _rgba(c) if not c.startswith('$') else (c, 1.0)
    if c.startswith('$'):
        name, _, al = c[1:].partition('/')
        return f"p.{name}" + (f".copy(alpha = {al}f)" if al else '')
    col, a = _rgba(c)
    h = col.lstrip('#').upper()
    if a >= 0.999: return f"Color(0xFF{h})"
    return f"Color(0x{int(round(a*255)):02X}{h})"

def _kstops(st):
    return ', '.join(f"{o}f to {_kcolor(c)}" for o, c in st)

def _kpaint(p):
    if isinstance(p, str): return f"SolidColor({_kcolor(p)})"
    if isinstance(p, Lin):
        x1, y1, x2, y2 = p.a
        return f"Brush.linearGradient({_kstops(p.stops)}, start = Offset({x1}f, {y1}f), end = Offset({x2}f, {y2}f))"
    if isinstance(p, Rad):
        cx, cy, r = p.c
        return f"Brush.radialGradient({_kstops(p.stops)}, center = Offset({cx}f, {cy}f), radius = {r}f)"
    raise ValueError(p)

def kotlin_layers(layers, indent):
    pad = ' ' * indent; out = []
    for L in layers:
        if L[0] == 'fill': out.append(f'{pad}LFill(path("{L[1]}"), {_kpaint(L[2])}),')
        elif L[0] == 'stroke': out.append(f'{pad}LStroke(path("{L[1]}"), {_kcolor(L[2])}, {L[3]}f),')
        elif L[0] == 'clip':
            out.append(f'{pad}LClip(path("{L[1]}"), listOf(')
            out.append(kotlin_layers(L[2], indent + 4))
            out.append(f'{pad})),')
    return '\n'.join(out)
