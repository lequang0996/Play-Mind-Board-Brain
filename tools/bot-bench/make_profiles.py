#!/usr/bin/env python3
"""Sinh hồ sơ game (BOT_GUIDE mục 12.2) vào docs/games/<id>.md từ bảng dưới và các đợt đo trong results/*.txt
(xem make_reports.py; đợt sau ghi đè kết quả đợt trước của cùng cặp).
"""
import glob, os, re, sys
ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
OUT = os.path.join(ROOT, "docs", "games")
RES = os.path.join(os.path.dirname(__file__), "results")
os.makedirs(OUT, exist_ok=True)

NAMES = ["Dễ", "Trung bình", "Khá", "Giỏi", "Cao thủ", "Thách đấu", "Siêu việt"]
RANGE = {(1, 0): (0.93, 0.98), (2, 1): (0.81, 0.95), (3, 2): (0.85, 0.96), (4, 3): (0.64, 0.88), (5, 4): (0.64, 0.88), (6, 5): (0.76, 0.93)}

G = {
 "chess": dict(name="Cờ vua", rules="Luật FIDE: nhập thành, bắt tốt qua đường, phong cấp; hòa khi hết nước (pat), luật 50 nước, thiếu quân. Luật riêng của app: nước tạo lại thế đã có (cùng bên tới lượt) thì bên vừa đi THUA.",
   test="perft (thế ban đầu độ sâu 1-4, Kiwipete 1-3, pos3, pos4) + thế viết tay trong SelfCheck.kt", ms=8000,
   source="tự làm (alpha-beta PVS, bảng băm, LMR, null-move, SEE). Stockfish GPL-3.0 chỉ làm engine đo; các ứng viên MIT (Leorik, Zahak, Arasan, Caissa) chưa qua cổng kỹ thuật. Tra cứu 27/11/2026.",
   opening="2-4 nước ngẫu nhiên có seed (LevelMatch.kt), chưa lọc cân bằng bằng Siêu việt", first="nhỏ; đấu cặp ván đổi màu",
   end="lặp thế = thua bên lặp; 50 nước; trần 1200 nửa nước trong bộ đo", w1="chiếu hết trong 1 nước (fastWin)", l1="nước để đối phương chiếu hết ngay, hoặc tự lặp thế (fastLosesNext)",
   ext="Stockfish UCI_Elo (tools/bot-bench/ChessMatch.kt), chưa chạy cho 7 cấp"),
 "xiangqi": dict(name="Cờ tướng", rules="Luật cờ tướng chuẩn: tướng không đối mặt, pháo ăn qua ngòi, mã/tượng bị cản; bên hết nước đi thua; hòa sau 120 nửa nước không ăn quân. Luật riêng: tạo lại thế đã có thì bên vừa đi THUA (thay luật chiếu dài/đuổi dài).",
   test="perft thế ban đầu độ sâu 1-3 + thế viết tay trong SelfCheck.kt", ms=8000,
   source="tự làm (cùng khung alpha-beta với cờ vua). Pikafish GPL-3.0 + mạng có điều khoản riêng: chỉ làm engine đo. Tra cứu 27/11/2026.",
   opening="2-4 nước ngẫu nhiên có seed", first="nhỏ; cặp ván đổi màu", end="lặp thế = thua; 120 nửa nước không ăn quân = hòa",
   w1="chiếu bí / làm đối phương hết nước trong 1 nước", l1="nước để đối phương thắng ngay hoặc tự lặp thế", ext="Pikafish theo số nút (XiangqiVsPikafish.kt), chưa chạy cho 7 cấp"),
 "caro": dict(name="Cờ caro (bàn vô hạn)", rules="5 quân liên tiếp trở lên thắng, trừ khi cả hai đầu của chuỗi cực đại bị quân đối phương chặn. Bàn không giới hạn (bot tính trên cửa sổ 15-24 ô quanh các quân).",
   test="thế viết tay trong SelfCheck.kt (caro vo han: ...)", ms=6000,
   source="tự làm (alpha-beta + VCF + phòng VCF; VCT có nhưng tắt). Rapfi GPL-3.0; engine Gomocup chơi luật khác. Tra cứu 27/11/2026.",
   opening="3 quân đầu ngẫu nhiên trong ô 5×5 quanh gốc", first="rất lớn: báo cáo riêng tỉ lệ điểm khi đi trước / đi sau", end="không có hòa (bàn vô hạn); trần 1200 nửa nước trong bộ đo",
   w1="hoàn thành năm (winCells)", l1="chặn bốn của đối phương (luôn chạy trước mọi thứ khác)", ext="không có engine đo đúng luật"),
 "quoridor": dict(name="Quoridor", rules="Bàn 9×9; 2 người mỗi bên 10 tường, 4 người mỗi bên 5 tường; nhảy thẳng/chéo qua quân; tường không được chặn kín đường. Luật riêng: 2 người, đi quân tạo lại thế đã có thì THUA; 4 người, nước đó bị cấm. Quá 300 nửa nước: hòa.",
   test="thế viết tay + phát lại lịch sử trong SelfCheck.kt", ms=7000,
   source="tự làm (alpha-beta PVS + bảng băm Zobrist + killer cho 2 người; paranoid cho 4 người). Không có engine chuẩn. Tra cứu 27/11/2026.",
   opening="2 nước đi quân ngẫu nhiên có seed", first="có; cặp ván đổi màu", end="lặp thế = thua (2 người) / cấm (4 người); 300 nửa nước = hòa",
   w1="đi quân tới cạnh đích", l1="nước để người kế tiếp đi quân tới đích ngay", ext="không có"),
 "wallgo": dict(name="Wall Go", rules="Bàn 7×7, 2-4 người; đặt quân theo thứ tự rắn; mỗi lượt đi 0-2 ô rồi bắt buộc đặt tường ở ô vừa đứng; hết ván khi mọi vùng chỉ còn quân của tối đa một người; lãnh thổ = số ô vùng chỉ có quân mình; hòa điểm xét vùng lớn nhất. Tường chỉ tăng nên không thể lặp thế.",
   test="thế viết tay + phát lại lịch sử trong SelfCheck.kt", ms=7000,
   source="tự làm (minimax alpha-beta lặp sâu dần, sắp nước theo đánh giá tĩnh; paranoid cho 3-4 người). Remake MIT có AI yếu; WallZero chưa rõ giấy phép. Tra cứu 27/11/2026.",
   opening="2 nước đặt quân ngẫu nhiên có seed", first="nhỏ; cặp ván đổi màu", end="theo luật lãnh thổ",
   w1="nước kết thúc ván mà mình thắng", l1="nước để người kế tiếp kết thúc ván mà mình không thắng", ext="không có"),
 "go": dict(name="Cờ vây 19×19", rules="Luật diện tích kiểu Tromp-Taylor, không gỡ quân chết (phải bắt hết trước khi bỏ lượt), cấm tự sát, siêu ko theo vị trí, komi 7,5, hai lần bỏ lượt kết thúc.",
   test="thế viết tay + phát lại lịch sử trong SelfCheck.kt", ms=15000,
   source="engine nền KataGo v1.18.2 (mã MIT) + mạng g170-b6c96 và g170e-b10c128 (CC0), xem docs/licenses. Dự phòng: MCTS-RAVE tự viết của core khi không chạy được KataGo.",
   opening="(chưa đo trên PC: không được chạy tệp KataGo tải về trên máy phát triển)", first="komi 7,5", end="hai lần bỏ lượt; trần size² × 3 nửa nước",
   w1="không áp dụng (mục 8.2): thay bằng bất biến cứng không tự lấp mắt", l1="không áp dụng; cấp Dễ/Trung bình lọc thêm nước tự atari", ext="GNU Go (tools/bench/go_vs_gnugo), chưa chạy cho 7 cấp"),
}

def load_results():
    res = {}
    for line in (l for p in sorted(glob.glob(os.path.join(RES, "*.txt"))) for l in open(p, encoding="utf-8")):
        m = re.match(r"(\w+) A=(\d) B=(\d) ms=(\d+) n=(\d+)\s+W/D/L=(\d+)/(\d+)/(\d+)\s+S=([\d.]+) \[([-\d.]+),([-\d.]+)\].*first=([\d.]+) second=([\d.]+)", line)
        if m:
            g, a, b = m.group(1), int(m.group(2)), int(m.group(3))
            res[(g, a, b)] = dict(ms=m.group(4), n=int(m.group(5)), w=m.group(6), d=m.group(7), l=m.group(8), s=float(m.group(9)),
                                 lo=float(m.group(10)), hi=float(m.group(11)), first=m.group(12), second=m.group(13))
    return res

def configs():
    src = open(os.path.join(ROOT, "core", "src", "main", "kotlin", "com", "fws", "playmind", "core", "BotLevels.kt"), encoding="utf-8").read()
    out = {}
    for gid in G:
        m = re.search(r"val %s = listOf\((.*?)\n    \)" % gid, src, re.S)
        if not m: continue
        items = re.findall(r"(d|n|top)\(([^()]*)\)", m.group(1))
        out[gid] = items
    return out

def describe(gid, kind, args):
    a = [x.strip() for x in args.split(",") if x.strip()]
    if kind == "top": return "toàn lực, thời gian botMillis" + ("; VCF tới %s" % a[0] if a and gid == "caro" else "")
    lim = ("độ sâu %s" % a[0]) if kind == "d" else ("%s lượt mô phỏng" % a[-1].split("=")[-1].strip() if gid == "go" else "%s nút" % a[0])
    rest = []
    if len(a) >= 3 and gid != "go" and a[1] not in ("0.0",): rest.append("nhiễu p = %s, δ = %s" % (a[1], a[2]))
    if gid == "caro" and len(a) >= 4: rest.append("VCF " + ("tắt" if a[3] == "0" else "tới %s" % a[3]))
    return lim + ("; " + "; ".join(rest) if rest else "")

res = load_results(); cfg = configs()
for gid, g in G.items():
    lines = [f"# Hồ sơ game: {g['name']}", "", "Theo mẫu BOT_GUIDE mục 12.2. Sinh bởi `tools/bot-bench/make_profiles.py`; số liệu đo nằm ở `tools/bot-bench/results/`, báo cáo chi tiết ở [docs/bot-results](../bot-results/README.md).", "",
             "| Mục | Nội dung |", "|---|---|",
             f"| Luật chốt | {g['rules']} |", f"| Kiểm thử luật | {g['test']} |",
             f"| botMillis (Siêu việt) | {g['ms']} ms trên điện thoại (Catalog.kt) |",
             "| Hệ số chậm điện thoại k | **chưa đo trên máy thật**; tạm giả định k = 4 (BotLevels.kt) |",
             f"| Lợi thế đi trước | {g['first']} |", f"| Nguồn bot | {g['source']} |", f"| Engine đo đối chiếu | {g['ext']} |",
             f"| Nguồn thế mở đầu | {g['opening']} |", f"| Điều kiện kết thúc | {g['end']} |",
             f"| win_in_1 / lose_in_1 | {g['w1']} / {g['l1']} |",
             "| g theo vùng nút | chưa đo |", "| Bộ thế chiến thuật / \"không được sai\" | chưa lập (chỉ có các thế trong SelfCheck.kt) |", "",
             "## 7 cấp (bảng cấu hình duy nhất: core/.../BotLevels.kt)", ""]
    if gid == "go":
        lines += ["Khi chạy KataGo (app/.../match/KataGo.kt):", "",
                  "| Cấp | Mạng | Lượt | Chọn nước |", "|---|---|---|---|",
                  "| Dễ | b6 | 1 lần chạy mạng | lấy mẫu theo chính sách, nhiệt độ 1,6, lọc tự lấp mắt / tự atari |",
                  "| Trung bình | b6 | 1 lần chạy mạng | như trên, nhiệt độ 0,8 |",
                  "| Khá | b6 | 12 | nhiệt độ 0,5 |", "| Giỏi | b10 | 40 | nhiệt độ 0,2 |",
                  "| Cao thủ | b10 | 160 | không nhiễu |", "| Thách đấu | b10 | 480 | không nhiễu |", "| Siêu việt | b10 | theo thời gian 15 s | không nhiễu |", "",
                  "Dự phòng MCTS khi không chạy được KataGo:", ""]
    lines += ["| Cấp | Cấu hình |", "|---|---|"]
    for i, (k, a) in enumerate(cfg.get(gid, [])):
        lines.append(f"| {NAMES[i]} | {describe(gid, k, a)} |")
    lines += ["", "## Kết quả 6 cặp kề (đo sơ bộ, CHƯA nghiệm thu)", ""]
    rows = [(a, b) for (a, b) in RANGE]
    have = [r for r in rows if (gid, r[0], r[1]) in res]
    if not have:
        lines.append("Chưa đo.")
    else:
        lines += ["| Cặp | Số ván | Thắng/Hòa/Thua | S | Khoảng tin cậy 95% | Khoảng đạt | Đi trước / đi sau | Kết luận |", "|---|---|---|---|---|---|---|---|"]
        for a, b in rows:
            r = res.get((gid, a, b))
            lo, hi = RANGE[(a, b)]
            if not r: lines.append(f"| {NAMES[a]} vs {NAMES[b]} | | | | | {lo:.0%}–{hi:.0%} | | chưa đo |"); continue
            ok = "trong khoảng" if lo <= r['s'] <= hi else ("thấp hơn khoảng (cấp quá sát nhau)" if r['s'] < lo else "cao hơn khoảng")
            lines.append(f"| {NAMES[a]} vs {NAMES[b]} | {r['n']} | {r['w']}/{r['d']}/{r['l']} | {r['s']:.3f} | {r['lo']:.2f}–{r['hi']:.2f} | {lo:.0%}–{hi:.0%} | {r['first']} / {r['second']} | {ok} |")
        lines += ["", "Siêu việt đo trên máy phát triển ở thời gian botMillis ÷ 4 (mục 3.2, k giả định). Số ván mỗi cặp ít hơn yêu cầu nghiệm thu (200, cặp Trung bình vs Dễ 400)."]
    lines += ["", "## Còn thiếu để nghiệm thu (mục 11.1)", "",
              "- Đo k và nút/giây trên điện thoại thật (máy mạnh và máy thấp nhất hỗ trợ); p99 thời gian nghĩ, RAM, nóng máy.",
              "- Đủ số ván cho 6 cặp kề và 5 cặp cách hai cấp + 3 cặp smoke; bộ thế mở đầu lọc cân bằng.",
              "- Bộ thế chiến thuật và bộ ≥ 30 thế \"không được sai\" chạy với cả 7 cấp; 1000 ván tự đấu trộn cấp.",
              "- Các cặp chưa vào khoảng đạt: theo guide mục 11.3 phải tăng sức Siêu việt, không hạ chuẩn."]
    open(os.path.join(OUT, f"{gid}.md"), "w", encoding="utf-8").write("\n".join(lines) + "\n")
print("profiles ->", OUT)
