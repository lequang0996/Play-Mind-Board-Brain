# Kết quả đo bot: Cờ caro

Sinh tự động bởi `tools/bot-bench/make_reports.py` từ `tools/bot-bench/results/`. Tiêu chí: [BOT_GUIDE](../BOT_GUIDE.md) mục 1.3 và 3.4.

## Tóm tắt

| Mục | Giá trị |
|---|---|
| Số đợt đo | 2 |
| Tổng số ván đã đấu | 1200 |
| Cặp kề đạt (kết quả mới nhất) | 1/6 |
| Nghiệm thu | Chưa đạt (cần đủ 6 cặp kề với đủ số ván, 5 cặp cách hai cấp và đo trên điện thoại thật) |

## Chỉ số thông minh (PM-Elo ước lượng)

Neo cấp Dễ = 800 theo thang quy ước của guide (mục 1.2), rồi cộng dồn khoảng Elo đo được giữa các cấp kề nhau. Đây là thang so sánh giữa các bot của app, **không phải điểm FIDE**. Cặp có tỉ lệ thắng ngoài 20%–80% cho Elo rất không chắc (đánh dấu ~).

| Cấp | Mục tiêu | Ước lượng | Lệch |
|---|---|---|---|
| Dễ | 800 | 800 | (neo) |
| Trung bình | 1350 | ~1174 | -176 |
| Khá | 1700 | ~1434 | -266 |
| Giỏi | 2100 | 1661 | -439 |
| Cao thủ | 2300 | 1724 | -576 |
| Thách đấu | 2500 | 1808 | -692 |
| Siêu việt | 2800 | 1863 | -937 |

## Kết quả mới nhất từng cặp kề

| Cặp | Đợt | Số ván (cần) | Thắng/Hòa/Thua | Tỉ lệ điểm | Khoảng tin cậy 95% | Khoảng đạt | Khoảng Elo (mục tiêu) | Kết luận |
|---|---|---|---|---|---|---|---|---|
| Trung bình vs Dễ | r2 | 100 (400) | 90/0/10 | 90,0% | 84,1%–95,9% | 93,0%–98,0% | ~+374 (550) | Cần thêm ván, sơ bộ |
| Khá vs Trung bình | r2 | 100 (200) | 82/0/18 | 82,0% | 74,4%–89,6% | 81,0%–95,0% | ~+260 (350) | Đạt, sơ bộ |
| Giỏi vs Khá | r2 | 100 (200) | 79/0/21 | 79,0% | 71,0%–87,0% | 85,0%–96,0% | +227 (400) | Cần thêm ván, sơ bộ |
| Cao thủ vs Giỏi | r2 | 100 (200) | 59/0/41 | 59,0% | 49,3%–68,7% | 64,0%–88,0% | +63 (200) | Cần thêm ván, sơ bộ |
| Thách đấu vs Cao thủ | r2 | 100 (200) | 62/0/38 | 62,0% | 52,4%–71,6% | 64,0%–88,0% | +84 (200) | Cần thêm ván, sơ bộ |
| Siêu việt vs Thách đấu | r2 | 100 (200) | 58/0/42 | 58,0% | 48,3%–67,7% | 76,0%–93,0% | +56 (300) | Không đạt (hai cấp quá sát), sơ bộ |

"Sơ bộ" = chưa đủ số ván guide yêu cầu nên kết luận chỉ để tham khảo.

## Lịch sử các đợt đo

### Đợt r1 (2026-11-27)

Đợt 1, cấu hình 7 cấp ban đầu (commit 918e36c, trước khi chỉnh). Siêu việt chạy botMillis ÷ 8 trên PC. Wall Go cặp Siêu việt vs Thách đấu, cờ tướng các cặp trên và cờ vua chưa chạy (dừng sớm để chạy đợt 2). LƯU Ý: tiến trình đợt 1 không dừng hẳn và chạy chồng lên đợt 2, nên các dòng cờ tướng của đợt này đo khi CPU quá tải (Siêu việt bị thiệt).

| Cặp | Thời gian Siêu việt (PC) | Số ván | Thắng/Hòa/Thua | Tỉ lệ điểm | Đi trước / đi sau | Kết luận |
|---|---|---|---|---|---|---|
| Trung bình vs Dễ | 875 ms | 100 | 93/0/7 | 93,0% | 0.98 / 0.88 | Đạt, sơ bộ |
| Khá vs Trung bình | 875 ms | 100 | 82/0/18 | 82,0% | 0.86 / 0.78 | Đạt, sơ bộ |
| Giỏi vs Khá | 875 ms | 100 | 75/0/25 | 75,0% | 0.96 / 0.54 | Không đạt (hai cấp quá sát), sơ bộ |
| Cao thủ vs Giỏi | 875 ms | 100 | 54/0/46 | 54,0% | 0.64 / 0.44 | Không đạt (hai cấp quá sát), sơ bộ |
| Thách đấu vs Cao thủ | 875 ms | 100 | 66/0/34 | 66,0% | 0.86 / 0.46 | Đạt, sơ bộ |
| Siêu việt vs Thách đấu | 875 ms | 100 | 61/0/39 | 61,0% | 0.86 / 0.36 | Không đạt (hai cấp quá sát), sơ bộ |

### Đợt r2 (2026-11-27)

Đợt 2, cấu hình đã chỉnh sau đợt 1 (Siêu việt nghĩ lâu hơn). Siêu việt chạy botMillis ÷ 4 trên PC (k = 4 giả định). LƯU Ý: cả đợt chạy chồng với tiến trình đợt 1 chưa dừng (22 tiến trình trên 12 luồng): các cấp giới hạn theo nút không bị ảnh hưởng, nhưng Siêu việt (giới hạn theo thời gian) chỉ được khoảng nửa sức tính, nên các cặp Siêu việt vs Thách đấu bị đánh giá thấp. Cờ tướng chưa xong, cờ vua chưa chạy (dừng để viết lại engine).

| Cặp | Thời gian Siêu việt (PC) | Số ván | Thắng/Hòa/Thua | Tỉ lệ điểm | Đi trước / đi sau | Kết luận |
|---|---|---|---|---|---|---|
| Trung bình vs Dễ | 1500 ms | 100 | 90/0/10 | 90,0% | 0.98 / 0.82 | Cần thêm ván, sơ bộ |
| Khá vs Trung bình | 1500 ms | 100 | 82/0/18 | 82,0% | 0.90 / 0.74 | Đạt, sơ bộ |
| Giỏi vs Khá | 1500 ms | 100 | 79/0/21 | 79,0% | 0.90 / 0.68 | Cần thêm ván, sơ bộ |
| Cao thủ vs Giỏi | 1500 ms | 100 | 59/0/41 | 59,0% | 0.68 / 0.50 | Cần thêm ván, sơ bộ |
| Thách đấu vs Cao thủ | 1500 ms | 100 | 62/0/38 | 62,0% | 0.76 / 0.48 | Cần thêm ván, sơ bộ |
| Siêu việt vs Thách đấu | 1500 ms | 100 | 58/0/42 | 58,0% | 0.78 / 0.38 | Không đạt (hai cấp quá sát), sơ bộ |

