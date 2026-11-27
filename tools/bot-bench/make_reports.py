#!/usr/bin/env python3
"""Sinh báo cáo kết quả đo bot vào docs/bot-results/ (README.md + <game>.md) từ tools/bot-bench/results/*.txt.

Mỗi tệp results/<đợt>.txt là một đợt đo:
  # date: 2026-11-27
  # note: mô tả đợt (cấu hình, thời gian)
  <các dòng bench.py in ra>, ví dụ
  chess A=1 B=0 ms=2000 n=100  W/D/L=93/1/6  S=0.935 [0.887,0.983] ... first=0.95 second=0.92 ...

Chạy:  python tools/bot-bench/make_reports.py
Tiêu chí lấy từ docs/BOT_GUIDE.md mục 1.2, 1.3, 2.3, 3.4.
"""
import glob, math, os, re

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
RES_DIR = os.path.join(os.path.dirname(__file__), "results")
OUT = os.path.join(ROOT, "docs", "bot-results")

NAMES = ["Dễ", "Trung bình", "Khá", "Giỏi", "Cao thủ", "Thách đấu", "Siêu việt"]
TARGET = [800, 1350, 1700, 2100, 2300, 2500, 2800]             # PM-Elo quy ước, mục 1.2
RANGE = {1: (0.93, 0.98), 2: (0.81, 0.95), 3: (0.85, 0.96), 4: (0.64, 0.88), 5: (0.64, 0.88), 6: (0.76, 0.93)}  # mục 1.3, khóa = cấp cao
NEED = {1: 400, 2: 200, 3: 200, 4: 200, 5: 200, 6: 200}       # số ván tối thiểu, mục 3.4
GAMES = [("chess", "Cờ vua"), ("xiangqi", "Cờ tướng"), ("caro", "Cờ caro"), ("quoridor", "Quoridor"), ("wallgo", "Wall Go"), ("go", "Cờ vây")]
GAUGE = re.compile(r"gauge (\w+) elo=(\d+) ms=(\d+) sfms=(\d+)(?: threads=(\d+))? n=(\d+)\s+W/D/L=(\d+)/(\d+)/(\d+)\s+S=([\d.]+) \[([-\d.]+),([-\d.]+)\]")
TARGET_EXT = {"chess": 2800}   # mục tiêu Elo so với engine ngoài (yêu cầu của chủ dự án: cờ vua 2800+)
LINE = re.compile(r"(\w+) A=(\d) B=(\d) ms=(\d+) n=(\d+)\s+W/D/L=(\d+)/(\d+)/(\d+)\s+S=([\d.]+) \[([-\d.]+),([-\d.]+)\].*first=([\d.]+) second=([\d.]+)")


def load_runs():
    runs = []
    for p in sorted(glob.glob(os.path.join(RES_DIR, "*.txt"))):
        tag = os.path.splitext(os.path.basename(p))[0]
        meta, rows, gauges = {}, [], []
        for line in open(p, encoding="utf-8"):
            if line.startswith("#"):
                k, _, v = line[1:].partition(":"); meta[k.strip()] = v.strip(); continue
            g = GAUGE.match(line)
            if g:
                w, d, l = int(g.group(7)), int(g.group(8)), int(g.group(9))
                gauges.append(dict(game=g.group(1), elo=int(g.group(2)), ms=int(g.group(3)), sfms=int(g.group(4)), threads=int(g.group(5) or 1),
                                   n=int(g.group(6)), w=w, d=d, l=l, s=float(g.group(10)), lo=float(g.group(11)), hi=float(g.group(12))))
                continue
            m = LINE.match(line)
            if not m: continue
            w, d, l = int(m.group(6)), int(m.group(7)), int(m.group(8))
            rows.append(dict(game=m.group(1), a=int(m.group(2)), b=int(m.group(3)), ms=int(m.group(4)), n=int(m.group(5)),
                             w=w, d=d, l=l, s=float(m.group(9)), lo=float(m.group(10)), hi=float(m.group(11)),
                             first=m.group(12), second=m.group(13)))
        runs.append(dict(tag=tag, date=meta.get("date", ""), note=meta.get("note", ""), rows=rows, gauges=gauges))
    return runs


def verdict(r):
    """Quy tắc mục 3.4: S trong khoảng đạt → đạt; khoảng tin cậy không chạm khoảng đạt → không đạt; còn lại cần thêm ván."""
    lo, hi = RANGE[r["a"]]
    if lo <= r["s"] <= hi: v = "Đạt"
    elif r["hi"] < lo: v = "Không đạt (hai cấp quá sát)"
    elif r["lo"] > hi: v = "Không đạt (hai cấp quá xa)"
    else: v = "Cần thêm ván"
    if r["n"] < NEED[r["a"]]: v += ", sơ bộ"
    return v


def elo_gap(r):
    """Khoảng Elo của cấp cao so với cấp thấp. Làm trơn +0,5 thắng/+0,5 thua để 100% không ra vô cực (ước lượng thô)."""
    s = (r["w"] + r["d"] / 2 + 0.5) / (r["n"] + 1)
    return 400 * math.log10(s / (1 - s)), 0.2 <= r["s"] <= 0.8


def latest(runs, game):
    """Kết quả mới nhất của từng cặp kề (đợt sau ghi đè đợt trước)."""
    out = {}
    for run in runs:
        for r in run["rows"]:
            if r["game"] == game and r["a"] == r["b"] + 1: out[r["a"]] = (run, r)
    return out


def ladder(lat):
    """'Chỉ số thông minh' (PM-Elo ước lượng): neo Dễ = 800 như thang quy ước, cộng dồn khoảng Elo đo được giữa các cấp kề."""
    vals, rough = [TARGET[0]], False
    for a in range(1, 7):
        if a not in lat: return vals, rough
        g, ok = elo_gap(lat[a][1]); rough |= not ok
        vals.append(vals[-1] + g)
    return vals, rough


def gauge_latest(runs, game):
    """Đợt đo Stockfish mới nhất của game (mọi mức Elo trong đợt đó)."""
    last = None
    for run in runs:
        gs = [g for g in run["gauges"] if g["game"] == game]
        if gs: last = (run, gs)
    return last


def gauge_pass(gs, tgt):
    """Đạt khi điểm ≥ 50% trước Stockfish ở một mức UCI_Elo ≥ mục tiêu, với ít nhất 40 ván."""
    return any(g["elo"] >= tgt and g["n"] >= 40 and g["s"] >= 0.5 for g in gs)


def gauge_estimate(gs):
    """Elo ước lượng gộp: trung bình có trọng số (n·S·(1−S)) của Elo suy ra từ các mức có điểm trong 20%–80%
    (ngoài vùng này thang UCI_Elo lệch nhiều và Elo suy ra không tin được, guide mục 2.1)."""
    num = den = 0.0
    use = [g for g in gs if 0.2 <= g["s"] <= 0.8] or gs
    for g in use:
        s = (g["w"] + g["d"] / 2 + 0.5) / (g["n"] + 1)
        if not (0.03 < s < 0.97): continue
        e = g["elo"] + 400 * math.log10(s / (1 - s)); wt = g["n"] * s * (1 - s)
        num += e * wt; den += wt
    return num / den if den else None


def pct(x): return f"{x * 100:.1f}%".replace(".", ",")


def game_page(gid, name, runs):
    lat = latest(runs, gid)
    mine = [(run, [r for r in run["rows"] if r["game"] == gid]) for run in runs]
    gauged = [run for run in runs if any(g["game"] == gid for g in run["gauges"])]
    nruns = len({run["tag"] for run, rows in mine if rows} | {run["tag"] for run in gauged})
    mine = [(run, rows) for run, rows in mine if rows]
    total = sum(r["n"] for _, rows in mine for r in rows) + sum(g["n"] for run in gauged for g in run["gauges"] if g["game"] == gid)
    passed = sum(1 for a in lat if verdict(lat[a][1]).startswith("Đạt"))
    L = [f"# Kết quả đo bot: {name}", "",
         "Sinh tự động bởi `tools/bot-bench/make_reports.py` từ `tools/bot-bench/results/`. Tiêu chí: [BOT_GUIDE](../BOT_GUIDE.md) mục 1.3 và 3.4.", "",
         "## Tóm tắt", "",
         "| Mục | Giá trị |", "|---|---|",
         f"| Số đợt đo | {nruns} |",
         f"| Tổng số ván đã đấu | {total} |",
         f"| Cặp kề đạt (kết quả mới nhất) | {passed}/6 |"]
    if gid == "go":
        L += ["| Ghi chú | Chưa đo: KataGo chỉ chạy được trong bản build Android (CI / điện thoại thật). |"]
    nacc = sum(1 for a in lat if verdict(lat[a][1]) == "Đạt")
    L += [f"| Nghiệm thu | {'Đạt' if nacc == 6 else 'Chưa đạt'} (cần đủ 6 cặp kề với đủ số ván, 5 cặp cách hai cấp và đo trên điện thoại thật) |", ""]

    gl = gauge_latest(runs, gid)
    ext = None
    if gl:
        run, gs = gl; ext = gauge_estimate(gs)
        tgt = TARGET_EXT.get(gid)
        L += ["## Đối chiếu Stockfish (Elo so với engine ngoài)", "",
              f"Đợt `{run['tag']}` ({run['date']}). Siêu việt đấu Stockfish 17.1 ở chế độ giới hạn sức `UCI_Elo`, Stockfish 1 luồng, bot {gs[0]['threads']} luồng, mỗi bên {gs[0]['ms']} ms/nước "
              "(máy CI; tương ứng botMillis trên điện thoại với hệ số chậm k giả định, guide mục 3.2). Luật lặp thế của app tính là hòa khi đo.", "",
              "| Stockfish UCI_Elo | Số ván | Thắng/Hòa/Thua | Điểm | Elo suy ra |", "|---|---|---|---|---|"]
        for g in sorted(gs, key=lambda g: g["elo"]):
            sm = (g["w"] + g["d"] / 2 + 0.5) / (g["n"] + 1)
            e = f"{g['elo'] + 400 * math.log10(sm / (1 - sm)):.0f}" if 0.03 < sm < 0.97 else "ngoài vùng đo"
            L.append(f"| {g['elo']} | {g['n']} | {g['w']}/{g['d']}/{g['l']} | {pct(g['s'])} | {e} |")
        if ext is not None:
            vtxt = "" if tgt is None else (f" → **{'ĐẠT' if gauge_pass(gs, tgt) else 'CHƯA ĐẠT'} mục tiêu {tgt}+** (tiêu chí: điểm ≥ 50% trước Stockfish UCI_Elo ≥ {tgt}, ≥ 40 ván)")
            L += ["", f"**Elo ước lượng gộp: {ext:.0f}**{vtxt}. Thang `UCI_Elo` của Stockfish neo theo CCRL, không phải điểm FIDE; đây là thước đo gần nhất có thể chạy tự động."]
        L.append("")

    vals, rough = ladder(lat)
    L += ["## Chỉ số thông minh (PM-Elo ước lượng)", "",
          "Neo cấp Dễ = 800 theo thang quy ước của guide (mục 1.2), rồi cộng dồn khoảng Elo đo được giữa các cấp kề nhau. "
          "Đây là thang so sánh giữa các bot của app, **không phải điểm FIDE**. Cặp có tỉ lệ thắng ngoài 20%–80% cho Elo rất không chắc (đánh dấu ~).", "",
          "| Cấp | Mục tiêu | Ước lượng | Lệch |", "|---|---|---|---|"]
    for i in range(7):
        if i < len(vals):
            unsure = i > 0 and not elo_gap(lat[i][1])[1]
            est = f"{'~' if unsure else ''}{vals[i]:.0f}"
            diff = f"{vals[i] - TARGET[i]:+.0f}" if i > 0 else "(neo)"
        else:
            est, diff = "chưa đo", ""
        L.append(f"| {NAMES[i]} | {TARGET[i]} | {est} | {diff} |")
    L.append("")

    L += ["## Kết quả mới nhất từng cặp kề", "",
          "| Cặp | Đợt | Số ván (cần) | Thắng/Hòa/Thua | Tỉ lệ điểm | Khoảng tin cậy 95% | Khoảng đạt | Khoảng Elo (mục tiêu) | Kết luận |",
          "|---|---|---|---|---|---|---|---|---|"]
    for a in range(1, 7):
        lo, hi = RANGE[a]; tgt = TARGET[a] - TARGET[a - 1]
        if a not in lat:
            L.append(f"| {NAMES[a]} vs {NAMES[a - 1]} | | ({NEED[a]}) | | | | {pct(lo)}–{pct(hi)} | ({tgt}) | Chưa đo |"); continue
        run, r = lat[a]; g, ok = elo_gap(r)
        L.append(f"| {NAMES[a]} vs {NAMES[a - 1]} | {run['tag']} | {r['n']} ({NEED[a]}) | {r['w']}/{r['d']}/{r['l']} | {pct(r['s'])} | "
                 f"{pct(r['lo'])}–{pct(r['hi'])} | {pct(lo)}–{pct(hi)} | {'' if ok else '~'}{g:+.0f} ({tgt}) | {verdict(r)} |")
    L += ["", "\"Sơ bộ\" = chưa đủ số ván guide yêu cầu nên kết luận chỉ để tham khảo.", ""]

    L += ["## Lịch sử các đợt đo", ""]
    if not mine: L.append("Chưa có đợt đo nào.")
    for run, rows in mine:
        L += [f"### Đợt {run['tag']} ({run['date']})", "", run["note"], "",
              "| Cặp | Thời gian Siêu việt (PC) | Số ván | Thắng/Hòa/Thua | Tỉ lệ điểm | Đi trước / đi sau | Kết luận |", "|---|---|---|---|---|---|---|"]
        for r in sorted(rows, key=lambda r: (r["a"] - r["b"], r["a"])):
            if r["a"] != r["b"] + 1: continue
            L.append(f"| {NAMES[r['a']]} vs {NAMES[r['b']]} | {r['ms']} ms | {r['n']} | {r['w']}/{r['d']}/{r['l']} | {pct(r['s'])} | {r['first']} / {r['second']} | {verdict(r)} |")
        L.append("")
    return "\n".join(L) + "\n", dict(passext=(gauge_pass(gl[1], TARGET_EXT[gid]) if gl and gid in TARGET_EXT else None), runs=nruns, total=total, passed=passed, measured=len(lat), sv=vals[6] if len(vals) == 7 else None, rough=rough, ext=ext)


def main():
    os.makedirs(OUT, exist_ok=True)
    runs = load_runs()
    idx = ["# Kết quả đo bot", "",
           "Mỗi game có một trang ghi: đã đo bao nhiêu đợt, bao nhiêu ván, từng cặp cấp có đạt khoảng của [BOT_GUIDE](../BOT_GUIDE.md) không, và chỉ số thông minh (PM-Elo ước lượng) của 7 cấp.",
           "Sinh tự động bởi `python tools/bot-bench/make_reports.py` từ `tools/bot-bench/results/*.txt` (mỗi tệp là một đợt đo). Không sửa tay.", "",
           "| Game | Số đợt | Tổng ván | Cặp kề đã đo | Cặp kề đạt | Siêu việt (PM-Elo, mục tiêu 2800) | Siêu việt so với Stockfish |", "|---|---|---|---|---|---|---|"]
    for gid, name in GAMES:
        text, s = game_page(gid, name, runs)
        open(os.path.join(OUT, f"{gid}.md"), "w", encoding="utf-8", newline="\n").write(text)
        sv = "chưa đủ dữ liệu" if s["sv"] is None else f"{'~' if s['rough'] else ''}{s['sv']:.0f}"
        ext = "chưa đo" if s["ext"] is None else f"{s['ext']:.0f}" + (f" ({'đạt' if s['passext'] else 'chưa đạt'} {TARGET_EXT[gid]}+)" if gid in TARGET_EXT else "")
        idx.append(f"| [{name}]({gid}.md) | {s['runs']} | {s['total']} | {s['measured']}/6 | {s['passed']}/6 | {sv} | {ext} |")
    idx += ["",
            "## Lưu ý", "",
            "- Các đợt hiện tại đo trên máy phát triển, Siêu việt chạy `botMillis ÷ k` với k giả định (chưa đo trên điện thoại thật, guide mục 3.2).",
            "- Mỗi cặp mới có 100 ván, ít hơn yêu cầu nghiệm thu (200, riêng Trung bình vs Dễ 400), nên kết luận là sơ bộ.",
            "- Chưa đo 5 cặp cách hai cấp (guide mục 1.3) và chưa đối chiếu engine ngoài (mục 3.5).",
            "- ~ = ước lượng thô vì có cặp thắng ngoài khoảng 20%–80%."]
    open(os.path.join(OUT, "README.md"), "w", encoding="utf-8", newline="\n").write("\n".join(idx) + "\n")
    print("reports ->", OUT)


if __name__ == "__main__":
    main()
