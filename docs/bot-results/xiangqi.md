# Kết quả đo bot: Cờ tướng

Sinh tự động bởi `tools/bot-bench/make_reports.py` từ `tools/bot-bench/results/`. Tiêu chí: [BOT_GUIDE](../BOT_GUIDE.md) mục 1.3 và 3.4.

## Tóm tắt

| Mục | Giá trị |
|---|---|
| Số đợt đo | 2 |
| Tổng số ván đã đấu | 400 |
| Cặp kề đạt (kết quả mới nhất) | 1/6 |
| Nghiệm thu | Chưa đạt (cần đủ 6 cặp kề với đủ số ván, 5 cặp cách hai cấp và đo trên điện thoại thật) |

## Chỉ số thông minh (PM-Elo ước lượng)

Neo cấp Dễ = 800 theo thang quy ước của guide (mục 1.2), rồi cộng dồn khoảng Elo đo được giữa các cấp kề nhau. Đây là thang so sánh giữa các bot của app, **không phải điểm FIDE**. Cặp có tỉ lệ thắng ngoài 20%–80% cho Elo rất không chắc (đánh dấu ~).

| Cấp | Mục tiêu | Ước lượng | Lệch |
|---|---|---|---|
| Dễ | 800 | 800 | (neo) |
| Trung bình | 1350 | ~1721 | +371 |
| Khá | 1700 | ~2172 | +472 |
| Giỏi | 2100 | chưa đo |  |
| Cao thủ | 2300 | chưa đo |  |
| Thách đấu | 2500 | chưa đo |  |
| Siêu việt | 2800 | chưa đo |  |

## Kết quả mới nhất từng cặp kề

| Cặp | Đợt | Số ván (cần) | Thắng/Hòa/Thua | Tỉ lệ điểm | Khoảng tin cậy 95% | Khoảng đạt | Khoảng Elo (mục tiêu) | Kết luận |
|---|---|---|---|---|---|---|---|---|
| Trung bình vs Dễ | r2 | 100 (400) | 100/0/0 | 100,0% | 100,0%–100,0% | 93,0%–98,0% | ~+921 (550) | Không đạt (hai cấp quá xa), sơ bộ |
| Khá vs Trung bình | r2 | 100 (200) | 93/1/6 | 93,5% | 88,7%–98,3% | 81,0%–95,0% | ~+451 (350) | Đạt, sơ bộ |
| Giỏi vs Khá | | (200) | | | | 85,0%–96,0% | (400) | Chưa đo |
| Cao thủ vs Giỏi | | (200) | | | | 64,0%–88,0% | (200) | Chưa đo |
| Thách đấu vs Cao thủ | | (200) | | | | 64,0%–88,0% | (200) | Chưa đo |
| Siêu việt vs Thách đấu | | (200) | | | | 76,0%–93,0% | (300) | Chưa đo |

"Sơ bộ" = chưa đủ số ván guide yêu cầu nên kết luận chỉ để tham khảo.

## Lịch sử các đợt đo

### Đợt r1 (2026-11-27)

Đợt 1, cấu hình 7 cấp ban đầu (commit 918e36c, trước khi chỉnh). Siêu việt chạy botMillis ÷ 8 trên PC. Wall Go cặp Siêu việt vs Thách đấu, cờ tướng các cặp trên và cờ vua chưa chạy (dừng sớm để chạy đợt 2).

| Cặp | Thời gian Siêu việt (PC) | Số ván | Thắng/Hòa/Thua | Tỉ lệ điểm | Đi trước / đi sau | Kết luận |
|---|---|---|---|---|---|---|
| Trung bình vs Dễ | 1125 ms | 100 | 100/0/0 | 100,0% | 1.00 / 1.00 | Không đạt (hai cấp quá xa), sơ bộ |
| Khá vs Trung bình | 1125 ms | 100 | 93/1/6 | 93,5% | 0.95 / 0.92 | Đạt, sơ bộ |

### Đợt r2 (2026-11-27)

Đợt 2, cấu hình đã chỉnh sau đợt 1 (Siêu việt nghĩ lâu hơn). Siêu việt chạy botMillis ÷ 4 trên PC (k = 4 giả định).

| Cặp | Thời gian Siêu việt (PC) | Số ván | Thắng/Hòa/Thua | Tỉ lệ điểm | Đi trước / đi sau | Kết luận |
|---|---|---|---|---|---|---|
| Trung bình vs Dễ | 2000 ms | 100 | 100/0/0 | 100,0% | 1.00 / 1.00 | Không đạt (hai cấp quá xa), sơ bộ |
| Khá vs Trung bình | 2000 ms | 100 | 93/1/6 | 93,5% | 0.95 / 0.92 | Đạt, sơ bộ |

