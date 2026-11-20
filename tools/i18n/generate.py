#!/usr/bin/env python3
"""Sinh app/src/main/res/values*/strings.xml từ các file *.txt (định dạng key|text) + bảng số nhiều."""
import os, re, sys
here = os.path.dirname(os.path.abspath(__file__))
res = os.path.join(here, "..", "..", "app", "src", "main", "res")
LANGS = {"en": "values", "zh": "values-zh-rCN", "es": "values-es", "pt": "values-pt", "ru": "values-ru", "ja": "values-ja", "ko": "values-ko", "vi": "values-vi"}

DIAMONDS = {
    "en": {"one": "+%d diamond", "other": "+%d diamonds"},
    "zh": {"other": "+%d 颗钻石"},
    "es": {"one": "+%d diamante", "other": "+%d diamantes"},
    "pt": {"one": "+%d diamante", "other": "+%d diamantes"},
    "ru": {"one": "+%d алмаз", "few": "+%d алмаза", "many": "+%d алмазов", "other": "+%d алмаза"},
    "ja": {"other": "+%d ダイヤ"}, "ko": {"other": "+%d 다이아몬드"}, "vi": {"other": "+%d kim cương"},
}
STREAK_DAYS = {
    "en": {"one": "%d-day streak", "other": "%d-day streak"},
    "zh": {"other": "连续签到 %d 天"},
    "es": {"one": "Racha de %d día", "other": "Racha de %d días"},
    "pt": {"one": "Sequência de %d dia", "other": "Sequência de %d dias"},
    "ru": {"one": "Серия: %d день", "few": "Серия: %d дня", "many": "Серия: %d дней", "other": "Серия: %d дня"},
    "ja": {"other": "%d 日連続"}, "ko": {"other": "%d일 연속"}, "vi": {"other": "Chuỗi %d ngày"},
}
STREAK_BONUS = {
    "en": {"one": "Win-streak bonus: +%d", "other": "Win-streak bonus: +%d"},
    "zh": {"other": "连胜奖励：+%d"},
    "es": {"one": "Bonus de racha: +%d", "other": "Bonus de racha: +%d"},
    "pt": {"one": "Bônus de sequência: +%d", "other": "Bônus de sequência: +%d"},
    "ru": {"one": "Бонус за серию: +%d", "few": "Бонус за серию: +%d", "many": "Бонус за серию: +%d", "other": "Бонус за серию: +%d"},
    "ja": {"other": "連勝ボーナス：+%d"}, "ko": {"other": "연승 보너스: +%d"}, "vi": {"other": "Thưởng chuỗi thắng: +%d"},
}
PLURALS = {"check_in_reward": DIAMONDS, "reward_diamonds": DIAMONDS, "streak_days": STREAK_DAYS, "streak_bonus": STREAK_BONUS}

def esc(s):
    s = s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("'", "\\'").replace('"', '\\"')
    if s[:1] in "@?": s = "\\" + s
    return s

def load(lang):
    d = {}
    for line in open(os.path.join(here, lang + ".txt"), encoding="utf-8").read().splitlines():
        if not line.strip(): continue
        k, v = line.split("|", 1)
        d[k] = v
    return d

data = {l: load(l) for l in LANGS}
base = set(data["en"])
ok = True
for l, d in data.items():
    if set(d) != base:
        ok = False
        print(f"[{l}] thiếu: {sorted(base - set(d))}  thừa: {sorted(set(d) - base)}")
# đối chiếu với khóa mà code Kotlin đang dùng
used = set()
for root, _, files in os.walk(os.path.join(res, "..", "kotlin")):
    for f in files:
        if f.endswith(".kt"):
            used |= set(re.findall(r"R\.string\.([a-z_0-9]+)", open(os.path.join(root, f), encoding="utf-8").read()))
missing = used - base
if missing: ok = False; print("Code dùng nhưng chưa có chuỗi:", sorted(missing))
unused = base - used
if unused: print("(thông tin) chuỗi chưa dùng:", sorted(unused))
if not ok: sys.exit(1)

for l, folder in LANGS.items():
    out = os.path.join(res, folder); os.makedirs(out, exist_ok=True)
    lines = ['<?xml version="1.0" encoding="utf-8"?>', "<resources>"]
    if l == "en":
        lines.append('    <string name="app_name" translatable="false">Play Mind: Board &amp; Brain</string>')
    for k in sorted(base):
        v = data[l][k].replace("\\n", "\\n")
        lines.append(f'    <string name="{k}">{esc(v)}</string>')
    for name, tbl in PLURALS.items():
        lines.append(f'    <plurals name="{name}">')
        for q, t in tbl[l].items():
            lines.append(f'        <item quantity="{q}">{esc(t)}</item>')
        lines.append("    </plurals>")
    lines.append("</resources>")
    open(os.path.join(out, "strings.xml"), "w", encoding="utf-8").write("\n".join(lines) + "\n")
print("OK:", len(base), "chuỗi x", len(LANGS), "ngôn ngữ")
