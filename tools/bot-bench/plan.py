#!/usr/bin/env python3
"""Đọc tools/bot-bench/request.txt và in ma trận job cho .github/workflows/bot-bench.yml.

request.txt:
  tag=<tên đợt đo>            (bắt buộc, dùng làm tên tệp results/<tag>.txt)
  note=<mô tả đợt đo>
  gauge  chess elo=2800 games=40 ms=2000 sfms=2000 shards=2 [threads=1]
  ladder chess 6 5 pairs=100 ms=2000 shards=5 [players=2]
  ab     chess new ref games=200 ms=200 shards=4 [nodes=0]
Mọi dòng có thể thêm props=k1=v1;k2=v2 (thuộc tính hệ thống cho bộ đo, ví dụ props=pm.level.chess.5=n15000 để thử cấu hình cấp).
Dòng ladder có props là thử nghiệm: kết quả ghi riêng, không vào báo cáo nghiệm thu.
Mỗi shard là một máy CI (4 vCPU) chạy vài tiến trình song song (gauge: 2 khi threads=1, 1 khi bot dùng nhiều luồng;
ladder và ab: 3, hoặc 1 nếu ab có người chơi smpN).
"""
import json, os, sys

def parse(path):
    meta, jobs = {}, []
    for raw in open(path, encoding="utf-8"):
        line = raw.split("#", 1)[0].strip()
        if not line: continue
        if line.split()[0] in ("gauge", "ladder", "ab"):
            t = line.split(); kv = dict(x.split("=", 1) for x in t if "=" in x); pos = [x for x in t if "=" not in x]
            jobs.append((pos, kv))
        elif "=" in line:
            k, v = line.split("=", 1); meta[k.strip()] = v.strip()
    return meta, jobs

def matrix(path):
    meta, jobs = parse(path)
    inc = []
    for j, (pos, kv) in enumerate(jobs):
        kind, game = pos[0], pos[1]
        shards = int(kv.get("shards", "1"))
        pre = "".join(f"-D{p} " for p in kv.get("props", "").split(";") if p)
        if kind == "gauge":
            threads = int(kv.get("threads", "1")); streams = 2 if threads == 1 else 1; games = int(kv["games"])
            per = max(2, -(-games // (shards * streams)))
            per += per % 2
            for s in range(shards):
                cmds = [pre + f"gauge {game} SF {kv['elo']} {per} {kv['ms']} {kv.get('sfms', kv['ms'])} {1000 * j + 100 * s + 10 * k + 7} {threads}" for k in range(streams)]
                inc.append(dict(job=j, shard=s, label=f"{j}-gauge-{game}-{kv['elo']}-{s}", cmds="|".join(cmds)))
        elif kind == "ab":
            a, b = pos[2], pos[3]; games = int(kv["games"])
            streams = 1 if (a.startswith("smp") or b.startswith("smp")) else 3
            per = max(2, -(-games // (shards * streams))); per += per % 2
            for s in range(shards):
                cmds = [pre + f"ab {game} {a} {b} {per} {kv['ms']} {100000 * j + 1000 * (s * streams + k)} {kv.get('nodes', '0')}" for k in range(streams)]
                inc.append(dict(job=j, shard=s, label=f"{j}-ab-{game}-{a}v{b}-{s}", cmds="|".join(cmds)))
        else:
            a, b = pos[2], pos[3]; streams = 3; pairs = int(kv["pairs"]); players = kv.get("players", "2")
            per = max(1, -(-pairs // (shards * streams)))
            for s in range(shards):
                cmds = [pre + f"ladder {game} {a} {b} {per} {kv['ms']} {100000 * j + 1000 * (s * streams + k) + 1} {players}" for k in range(streams)]
                inc.append(dict(job=j, shard=s, label=f"{j}-ladder-{game}-{a}v{b}-{s}", cmds="|".join(cmds)))
    return meta, inc

if __name__ == "__main__":
    meta, inc = matrix(sys.argv[1] if len(sys.argv) > 1 else "tools/bot-bench/request.txt")
    print("matrix=" + json.dumps({"include": inc}))
    print("tag=" + meta["tag"])
