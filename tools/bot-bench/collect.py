#!/usr/bin/env python3
"""Gom kết quả các shard của bot-bench.yml thành tools/bot-bench/results/<tag>.txt (định dạng make_reports.py đọc).

  python3 tools/bot-bench/collect.py <request.txt> <partsDir> <date>
Mỗi tệp part có dòng đầu "# job=<n> ..." rồi các dòng "R ..." (ladder) hoặc "G ..." (gauge) và "TOTAL ...".
In bảng tóm tắt dạng Markdown ra stdout.
"""
import glob, math, os, re, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from plan import parse

def ci(w, d, l):
    n = w + d + l; s = (w + d / 2) / n
    var = (w * (1 - s) ** 2 + d * (0.5 - s) ** 2 + l * s ** 2) / max(1, n - 1)
    se = math.sqrt(var / n)
    return s, s - 1.96 * se, s + 1.96 * se

def main():
    req, parts, date = sys.argv[1], sys.argv[2], sys.argv[3]
    meta, jobs = parse(req)
    acc = {}
    for p in sorted(glob.glob(os.path.join(parts, "**", "*.txt"), recursive=True)):
        lines = open(p, encoding="utf-8").read().splitlines()
        if not lines or not lines[0].startswith("# job="): continue
        j = int(re.match(r"# job=(\d+)", lines[0]).group(1))
        a = acc.setdefault(j, dict(w=0, d=0, l=0, first=[], second=[], plies=[], reasons={}, cpu=lines[0], err=[], eng=""))
        for ln in lines[1:]:
            if ln.startswith("#! "):
                if len(a["err"]) < 15: a["err"].append(ln[3:].strip()[:200])
                continue
            if ln.startswith("# engine "): a["eng"] = ln[2:].strip(); continue
            t = ln.split()
            if not t: continue
            if t[0] in ("R", "G"):
                sc = float(t[3]); a["plies"].append(int(t[4])); a["reasons"][t[5]] = a["reasons"].get(t[5], 0) + 1
                first = (t[2] == "1") if t[0] == "R" else (t[2] == "w")
                (a["first"] if first else a["second"]).append(sc)
                if sc == 1.0: a["w"] += 1
                elif sc == 0.0: a["l"] += 1
                else: a["d"] += 1
    out = [f"# date: {date}", f"# note: {meta.get('note', '')}"]
    md = [f"### Kết quả đo `{meta['tag']}`", "", "| Phép đo | Ván | Thắng/Hòa/Thua | Điểm | Ghi chú |", "|---|---|---|---|---|"]
    for j, (pos, kv) in enumerate(jobs):
        a = acc.get(j)
        if a and a["err"]:
            out.append(f"# lỗi {' '.join(pos)}: " + " / ".join(a["err"]))
        if not a or a["w"] + a["d"] + a["l"] == 0:
            md.append(f"| {' '.join(pos)} | 0 | | | không có kết quả |"); continue
        w, d, l = a["w"], a["d"], a["l"]; n = w + d + l
        s, lo, hi = ci(w, d, l)
        f = sum(a["first"]) / max(1, len(a["first"])); sc2 = sum(a["second"]) / max(1, len(a["second"]))
        reasons = "{" + ", ".join(f"'{k}': {v}" for k, v in sorted(a["reasons"].items())) + "}"
        plies = sum(a["plies"]) // max(1, len(a["plies"]))
        props = kv.get("props", "")
        elo_ab = f"{400 * math.log10(s / (1 - s)):+.0f}" if 0.03 < s < 0.97 else "-"
        if pos[0] == "ab" or (pos[0] == "ladder" and props):
            # thử nghiệm: không theo định dạng nghiệm thu nên make_reports.py bỏ qua
            what = f"{pos[2]}-vs-{pos[3]}" if pos[0] == "ab" else f"{pos[2]}v{pos[3]}"
            extra = f" nodes={kv['nodes']}" if kv.get("nodes", "0") != "0" else ""
            out.append(f"exp {pos[0]} {pos[1]} {what} ms={kv['ms']}{extra} props={props or '-'} n={n}  W/D/L={w}/{d}/{l}  "
                       f"S={s:.3f} [{lo:.3f},{hi:.3f}]  elo={elo_ab}  plies~{plies}  {reasons}")
            md.append(f"| {pos[0]} {pos[1]}: {what} {props} | {n} | {w}/{d}/{l} | {s:.3f} | Elo {elo_ab} |")
        elif pos[0] == "ladder":
            elo = f"{400 * math.log10(s / (1 - s)):.0f}" if 0.2 <= s <= 0.8 else "-"
            out.append(f"{pos[1]} A={pos[2]} B={pos[3]} ms={kv['ms']} n={n}  W/D/L={w}/{d}/{l}  S={s:.3f} [{lo:.3f},{hi:.3f}]  elo={elo}  "
                       f"first={f:.2f} second={sc2:.2f}  plies~{plies}  {reasons}  ci")
            md.append(f"| {pos[1]}: cấp {pos[2]} vs cấp {pos[3]} | {n} | {w}/{d}/{l} | {s:.3f} | |")
        elif "nodes" in kv:
            eng = a["eng"] or "engine"
            out.append(f"exp gauge {pos[1]} nodes={kv['nodes']} ms={kv['ms']} threads={kv.get('threads', '1')} n={n}  W/D/L={w}/{d}/{l}  S={s:.3f} [{lo:.3f},{hi:.3f}]  elo={elo_ab}  plies~{plies}  {reasons}  ({eng})")
            md.append(f"| {pos[1]} vs {eng} | {n} | {w}/{d}/{l} | {s:.3f} | Elo tương đối {elo_ab} |")
        else:
            elo = int(kv["elo"])
            est = f"{elo + 400 * math.log10(s / (1 - s)):.0f}" if 0.03 < s < 0.97 else (">" if s >= 0.97 else "<") + str(elo)
            out.append(f"gauge {pos[1]} elo={elo} ms={kv['ms']} sfms={kv.get('sfms', kv['ms'])} threads={kv.get('threads', '1')} n={n}  W/D/L={w}/{d}/{l}  S={s:.3f} [{lo:.3f},{hi:.3f}]  est={est}  plies~{plies}  {reasons}")
            md.append(f"| {pos[1]} vs Stockfish UCI_Elo {elo} | {n} | {w}/{d}/{l} | {s:.3f} | Elo ước lượng {est} |")
    os.makedirs("tools/bot-bench/results", exist_ok=True)
    open(f"tools/bot-bench/results/{meta['tag']}.txt", "w", encoding="utf-8", newline="\n").write("\n".join(out) + "\n")
    print("\n".join(md))

if __name__ == "__main__":
    main()
