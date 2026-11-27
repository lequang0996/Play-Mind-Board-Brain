# Kết quả đo bot: Quoridor

Sinh tự động bởi `tools/bot-bench/make_reports.py` từ `tools/bot-bench/results/`. Tiêu chí: [BOT_GUIDE](../BOT_GUIDE.md) mục 1.3 và 3.4.

## Tóm tắt

| Mục | Giá trị |
|---|---|
| Số đợt đo | 2 |
| Tổng số ván đã đấu | 1200 |
| Cặp kề đạt (kết quả mới nhất) | 2/6 |
| Nghiệm thu | Chưa đạt (cần đủ 6 cặp kề với đủ số ván, 5 cặp cách hai cấp và đo trên điện thoại thật) |

## Chỉ số thông minh (PM-Elo ước lượng)

Neo cấp Dễ = 800 theo thang quy ước của guide (mục 1.2), rồi cộng dồn khoảng Elo đo được giữa các cấp kề nhau. Đây là thang so sánh giữa các bot của app, **không phải điểm FIDE**. Cặp có tỉ lệ thắng ngoài 20%–80% cho Elo rất không chắc (đánh dấu ~).

| Cấp | Mục tiêu | Ước lượng | Lệch |
|---|---|---|---|
| Dễ | 800 | 800 | (neo) |
| Trung bình | 1350 | ~1721 | +371 |
| Khá | 1700 | 1805 | +105 |
| Giỏi | 2100 | 1976 | -124 |
| Cao thủ | 2300 | 2090 | -210 |
| Thách đấu | 2500 | 2261 | -239 |
| Siêu việt | 2800 | 2423 | -377 |

## Kết quả mới nhất từng cặp kề

| Cặp | Đợt | Số ván (cần) | Thắng/Hòa/Thua | Tỉ lệ điểm | Khoảng tin cậy 95% | Khoảng đạt | Khoảng Elo (mục tiêu) | Kết luận |
|---|---|---|---|---|---|---|---|---|
| Trung bình vs Dễ | r2 | 100 (400) | 100/0/0 | 100,0% | 100,0%–100,0% | 93,0%–98,0% | ~+921 (550) | Không đạt (hai cấp quá xa), sơ bộ |
| Khá vs Trung bình | r2 | 100 (200) | 62/0/38 | 62,0% | 52,4%–71,6% | 81,0%–95,0% | +84 (350) | Không đạt (hai cấp quá sát), sơ bộ |
| Giỏi vs Khá | r2 | 100 (200) | 73/0/27 | 73,0% | 64,3%–81,7% | 85,0%–96,0% | +171 (400) | Không đạt (hai cấp quá sát), sơ bộ |
| Cao thủ vs Giỏi | r2 | 100 (200) | 66/0/34 | 66,0% | 56,7%–75,3% | 64,0%–88,0% | +114 (200) | Đạt, sơ bộ |
| Thách đấu vs Cao thủ | r2 | 100 (200) | 73/0/27 | 73,0% | 64,3%–81,7% | 64,0%–88,0% | +171 (200) | Đạt, sơ bộ |
| Siêu việt vs Thách đấu | r2 | 100 (200) | 72/0/28 | 72,0% | 63,2%–80,8% | 76,0%–93,0% | +162 (300) | Cần thêm ván, sơ bộ |

"Sơ bộ" = chưa đủ số ván guide yêu cầu nên kết luận chỉ để tham khảo.

## Lịch sử các đợt đo

### Đợt r1 (2026-11-27)

Đợt 1, cấu hình 7 cấp ban đầu (commit 918e36c, trước khi chỉnh). Siêu việt chạy botMillis ÷ 8 trên PC. Wall Go cặp Siêu việt vs Thách đấu, cờ tướng các cặp trên và cờ vua chưa chạy (dừng sớm để chạy đợt 2).

| Cặp | Thời gian Siêu việt (PC) | Số ván | Thắng/Hòa/Thua | Tỉ lệ điểm | Đi trước / đi sau | Kết luận |
|---|---|---|---|---|---|---|
| Trung bình vs Dễ | 875 ms | 100 | 79/0/21 | 79,0% | 0.76 / 0.82 | Không đạt (hai cấp quá sát), sơ bộ |
| Khá vs Trung bình | 875 ms | 100 | 67/0/33 | 67,0% | 0.62 / 0.72 | Không đạt (hai cấp quá sát), sơ bộ |
| Giỏi vs Khá | 875 ms | 100 | 60/0/40 | 60,0% | 0.58 / 0.62 | Không đạt (hai cấp quá sát), sơ bộ |
| Cao thủ vs Giỏi | 875 ms | 100 | 73/0/27 | 73,0% | 0.78 / 0.68 | Đạt, sơ bộ |
| Thách đấu vs Cao thủ | 875 ms | 100 | 60/0/40 | 60,0% | 0.42 / 0.78 | Cần thêm ván, sơ bộ |
| Siêu việt vs Thách đấu | 875 ms | 100 | 58/0/42 | 58,0% | 0.48 / 0.68 | Không đạt (hai cấp quá sát), sơ bộ |

### Đợt r2 (2026-11-27)

Đợt 2, cấu hình đã chỉnh sau đợt 1 (Siêu việt nghĩ lâu hơn). Siêu việt chạy botMillis ÷ 4 trên PC (k = 4 giả định).

| Cặp | Thời gian Siêu việt (PC) | Số ván | Thắng/Hòa/Thua | Tỉ lệ điểm | Đi trước / đi sau | Kết luận |
|---|---|---|---|---|---|---|
| Trung bình vs Dễ | 1750 ms | 100 | 100/0/0 | 100,0% | 1.00 / 1.00 | Không đạt (hai cấp quá xa), sơ bộ |
| Khá vs Trung bình | 1750 ms | 100 | 62/0/38 | 62,0% | 0.54 / 0.70 | Không đạt (hai cấp quá sát), sơ bộ |
| Giỏi vs Khá | 1750 ms | 100 | 73/0/27 | 73,0% | 0.70 / 0.76 | Không đạt (hai cấp quá sát), sơ bộ |
| Cao thủ vs Giỏi | 1750 ms | 100 | 66/0/34 | 66,0% | 0.70 / 0.62 | Đạt, sơ bộ |
| Thách đấu vs Cao thủ | 1750 ms | 100 | 73/0/27 | 73,0% | 0.80 / 0.66 | Đạt, sơ bộ |
| Siêu việt vs Thách đấu | 1750 ms | 100 | 72/0/28 | 72,0% | 0.78 / 0.66 | Cần thêm ván, sơ bộ |

