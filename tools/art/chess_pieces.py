"""Quân cờ vua kiểu tiện (lathe) bạc ngọc trai / đen huyền, viền & vòng vàng - theo ảnh tham chiếu."""
from dsl import *
import math

# Vật liệu: gradient hình trụ ngang (sáng ở trái-trên, tối ở mép)
GOLD = dict(edge='#6E470A', shade='#B07F14', mid='#DFB03A', hi='#FFF0B5')
BODY = dict(edge='$bEdge', shade='$bShade', mid='$bMid', hi='$bHi')

PAL_WHITE = dict(bEdge='#8089A3', bShade='#C3C9DA', bMid='#E6E9F1', bHi='#FFFFFF', ink='#3C4666')
PAL_BLACK = dict(bEdge='#030305', bShade='#14141B', bMid='#2A2A35', bHi='#6B6C80', ink='#EDE3C6')
KOTLIN_FIELDS = ['bEdge', 'bShade', 'bMid', 'bHi']

def cyl(x0, x1, m):
    return Lin(x0, 0, x1, 0, [(0, m['edge']), (.13, m['shade']), (.30, m['hi']), (.44, m['mid']), (.80, m['shade']), (1, m['edge'])])

def sphere(cx, cy, r, m):
    return Rad(cx - r * .35, cy - r * .4, r * 1.5, [(0, m['hi']), (.45, m['mid']), (.8, m['shade']), (1, m['edge'])])

def tier(yt, yb, rt, rb, m, bulge=0.0, ry=1.8, rim=True):
    """Một khối tròn xoay: rộng rt ở đỉnh, rb ở đáy, mép đáy cong xuống (nhìn từ trên xuống)."""
    mid = (yt + yb) / 2
    xm = (rt + rb) / 2 + bulge
    d = (f"M{f(50-rt)} {f(yt)}L{f(50+rt)} {f(yt)}Q{f(50+xm)} {f(mid)} {f(50+rb)} {f(yb)}"
         f"A{f(rb)} {f(ry)} 0 0 1 {f(50-rb)} {f(yb)}Q{f(50-xm)} {f(mid)} {f(50-rt)} {f(yt)}Z")
    layers = [fill(d, cyl(50 - max(rt, rb + bulge), 50 + max(rt, rb + bulge), m))]
    if rim:
        layers.append(stroke(f"M{f(50+rb)} {f(yb)}A{f(rb)} {f(ry)} 0 0 1 {f(50-rb)} {f(yb)}", '#000000/0.28', 0.9))
    return d, layers

def ribs(path, y0, y1, r, gap=3.6, slope=5.0, color='#6E470A/0.55'):
    ls = []
    x = 50 - r - slope
    while x < 50 + r + slope:
        ls.append(stroke(f"M{f(x)} {f(y1)}L{f(x+slope)} {f(y0)}", color, 1.15))
        ls.append(stroke(f"M{f(x+1.3)} {f(y1)}L{f(x+slope+1.3)} {f(y0)}", '#FFF0B5/0.45', 0.7))
        x += gap
    return clip(path, ls)

def band(path, y0, y1, r, m=GOLD):
    return clip(path, [fill(rect(50 - r - 1, y0, 2 * r + 2, y1 - y0), cyl(50 - r, 50 + r, m))])

def collar(yb, yt, rb, rt, spikes=6, depth=7.5):
    """Cổ áo vàng hình vương miện: các cánh hoa nhọn hướng lên (như ảnh tham chiếu)."""
    xs = [50 - rt + (2 * rt) * i / spikes for i in range(spikes + 1)]
    yn = yt + depth                                   # đáy các khe giữa cánh
    d = f"M{f(50-rb)} {f(yb)}A{f(rb)} 2 0 0 0 {f(50+rb)} {f(yb)}L{f(50+rt)} {f(yn)}"
    for i in range(spikes, 0, -1):
        x1, x0 = xs[i], xs[i - 1]; mx = (x0 + x1) / 2
        d += f"Q{f(x1 - (x1-x0)*.05)} {f(yt + depth*.35)} {f(mx)} {f(yt)}Q{f(x0 + (x1-x0)*.05)} {f(yt + depth*.35)} {f(x0)} {f(yn)}"
    d += 'Z'
    layers = [fill(d, cyl(50 - rb, 50 + rb, GOLD))]
    for i in range(spikes, 0, -1):
        mx = (xs[i] + xs[i - 1]) / 2
        k = (mx - 50) / max(rt, 1)
        layers.append(stroke(f"M{f(mx)} {f(yt+2.2)}Q{f(mx + k*1.5)} {f((yt+yb)/2 + 1)} {f(50 + k*rb*0.9)} {f(yb-0.6)}", '#6E470A/0.55', 0.9))
        layers.append(stroke(f"M{f(mx-0.9)} {f(yt+2.6)}Q{f(mx - 0.9 + k*1.5)} {f((yt+yb)/2 + 1)} {f(50 + k*rb*0.9 - 1)} {f(yb-0.6)}", '#FFF0B5/0.55', 0.6))
    layers.append(stroke(f"M{f(50-rb)} {f(yb)}A{f(rb)} 2 0 0 0 {f(50+rb)} {f(yb)}", '#000000/0.3', 0.8))
    return d, layers

def base(r=29):
    """Đế chung: đĩa tròn + vòng vàng mảnh + loe vàng có gân bện."""
    out = []
    d1, l1 = tier(83, 94, r, r, BODY, bulge=1.2, ry=2.6)
    out += l1
    out.append(band(d1, 88.6, 90.4, r + 2))
    d2, l2 = tier(69, 83.5, r - 9.5, r - 2, GOLD, bulge=-3.2, ry=2.2)
    out += l2
    out.append(ribs(d2, 69, 84, r, gap=3.8, slope=5.5))
    return out

def stem(yt, yb, rt, rb, bulge=-2.2):
    d, l = tier(yt, yb, rt, rb, BODY, bulge=bulge, ry=1.6)
    return l

# ───────────── Các quân ─────────────
def pawn():
    L = base(25)
    L += stem(51, 70, 8.5, 12.5)
    d, l = collar(57, 44, 17, 10, 6, 7); L += l
    head = lathe([('C', 53, 16, 55.5, 19, 58, 22), ('C', 67, 28, 67.5, 40, 60, 45), ('C', 58, 47, 56, 48, 55, 49)], 13.5, 49)
    L.append(fill(head, Rad(43, 28, 30, [(0, '$bHi'), (.4, '$bMid'), (.78, '$bShade'), (1, '$bEdge')])))
    L.append(band(head, 34, 37, 18))
    L.append(fill(ell(43.5, 31, 3.6, 7), '#FFFFFF/0.38'))
    return L

def rook():
    L = base(28)
    L += stem(46, 70, 10.5, 15)
    d, l = collar(54, 43, 16.5, 11, 6); L += l
    cup, l = tier(27, 46, 22.5, 12.5, BODY, bulge=0.5, ry=2.0); L += l
    d3, l = tier(22.5, 28.5, 24, 24, GOLD, bulge=0.3, ry=2.2); L += l
    # đỉnh tháp: dải thân + 5 răng cưa liền khối
    blk, l = tier(18, 24, 21.5, 21.5, BODY, bulge=0, ry=1.5, rim=False); L += l
    mw, gap = 8.2, 2.4
    x = 50 - (5 * mw + 4 * gap) / 2
    for i in range(5):
        L.append(fill(rect(x, 9.5, mw, 12, 0.9), cyl(28, 72, BODY)))
        L.append(stroke(f"M{f(x+0.4)} {f(9.5)}H{f(x+mw-0.4)}", '#FFFFFF/0.55', 0.9))
        L.append(stroke(f"M{f(x)} {f(10.4)}V{f(21.5)}", '#000000/0.18', 0.7))
        x += mw + gap
    return L

def knight():
    L = base(28)
    L += stem(56, 70, 13, 15.5)
    d, l = collar(60, 50, 17, 12, 6, 6.5); L += l
    head = ("M32 56C35 51 39 47 38 42C30 43 22 40 20 35C19 31 22 28 26 26C30 23 33 17 38 13L40 5L46 11"
            "C57 10 69 20 71 36C73 48 69 58 68 56Z")
    L.append(fill(head, Lin(24, 12, 70, 56, [(0, '$bHi'), (.45, '$bMid'), (.8, '$bShade'), (1, '$bEdge')])))
    # bờm vàng
    mane = ("M45 11C52 5 56 7 58 4C60 9 64 8 66 12C67 17 72 17 73 22C74 27 77 29 77 34C77 38 72 37 71 36"
            "C69 21 58 12 46 12Z")
    L.append(fill(mane, Lin(44, 5, 76, 36, [(0, '#FFF0B5'), (.5, '#DFB03A'), (1, '#8A5E0C')])))
    L.append(stroke("M48 14C58 15 66 22 69 33", '#6E470A/0.45', 0.9))
    L.append(fill(ell(33, 27, 2.1, 2.1), '$ink'))
    L.append(fill(ell(32.4, 26.4, .7, .7), '#FFFFFF/0.9'))
    L.append(fill(ell(23.5, 36, 1.4, 1.1), '$ink/0.8'))
    L.append(stroke("M26 32C30 31 34 33 36 37", '$ink/0.35', 0.9))
    L.append(fill(ell(40, 36, 5, 9), '#FFFFFF/0.18'))
    return L

def bishop():
    L = base(27)
    L += stem(50, 70, 9, 13.5)
    d, l = collar(54, 43, 16, 9.5, 6); L += l
    head = lathe([('C', 54.5, 14, 64, 21, 64, 33), ('C', 64, 42, 58, 46, 56, 47)], 7.5, 47)
    L.append(fill(head, Rad(43, 27, 28, [(0, '$bHi'), (.4, '$bMid'), (.78, '$bShade'), (1, '$bEdge')])))
    L.append(band(head, 39.5, 42, 14))
    L.append(stroke("M45 19C52 22 57 28 58 33", '#000000/0.30', 1.6))
    L.append(stroke("M46.2 18.2C53 21 58.2 27 59.3 32", '#FFFFFF/0.55', 0.9))
    L.append(fill(ell(50, 7.2, 3.8, 3.8), sphere(50, 7.2, 3.8, GOLD)))
    L.append(fill(ell(44.5, 28, 3, 6), '#FFFFFF/0.30'))
    return L

def queen():
    L = base(29)
    L += stem(44, 70, 10.5, 15)
    d, l = collar(52, 40, 17, 11, 7); L += l
    cup, l = tier(22, 41, 23, 11.5, BODY, bulge=0.8, ry=2.0); L += l
    d3, l = tier(18.5, 24.5, 24.5, 24.5, GOLD, bulge=0.3, ry=2.2); L += l
    L.append(fill(ell(50, 19, 21.5, 2.8), '#000000/0.22'))
    for i in range(7):
        a = math.pi * (i / 6.0)
        x = 50 - 21 * math.cos(a); y = 18 + 2.6 * math.sin(a) * 0 + 0
        L.append(fill(ell(x, 15.5 - 1.5 * math.sin(a), 3.0, 3.0), sphere(x, 15.5 - 1.5 * math.sin(a), 3.0, GOLD)))
    L.append(fill(ell(50, 9.5, 4.2, 4.2), sphere(50, 9.5, 4.2, GOLD)))
    L.append(fill(ell(50, 9.5, 1.6, 1.6), '#C03040'))
    return L

def king():
    L = base(29)
    L += stem(46, 70, 10.5, 15)
    d, l = collar(54, 42, 17, 11, 7); L += l
    cup, l = tier(27, 44, 22.5, 12, BODY, bulge=0.6, ry=2.0); L += l
    d3, l = tier(22, 29.5, 25.5, 25.5, GOLD, bulge=0.3, ry=2.5); L += l
    dome, l = tier(15.5, 23, 11.5, 15, BODY, bulge=0.2, ry=1.8); L += l
    cross = "M45.5 0.5H54.5V6H61V14H54.5V20.5H45.5V14H39V6H45.5Z"
    L.append(fill(cross, Lin(39, 0, 61, 21, [(0, '$bHi'), (.5, '$bMid'), (1, '$bEdge')])))
    L.append(stroke(cross, '#000000/0.35', 0.8))
    L.append(stroke("M46.5 1.5H53.5", '#FFFFFF/0.7', 0.8))
    L.append(fill(ell(50, 10, 3, 3), sphere(50, 10, 3, GOLD)))
    return L

PIECES = {1: ('P', pawn), 2: ('N', knight), 3: ('B', bishop), 4: ('R', rook), 5: ('Q', queen), 6: ('K', king)}
