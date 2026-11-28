# Kết quả đo bot: Wall Go

Sinh tự động bởi `tools/bot-bench/make_reports.py` từ `tools/bot-bench/results/`. Tiêu chí: [BOT_GUIDE](../BOT_GUIDE.md) mục 1.3 và 3.4.

## Tóm tắt

| Mục | Giá trị |
|---|---|
| Số đợt đo | 3 |
| Tổng số ván đã đấu | 3056 |
| Cặp kề đạt (kết quả mới nhất) | 3/6 |
| Nghiệm thu | Chưa đạt (cần đủ 6 cặp kề với đủ số ván, 5 cặp cách hai cấp và đo trên điện thoại thật) |

## Chỉ số thông minh (PM-Elo ước lượng)

Neo cấp Dễ = 800 theo thang quy ước của guide (mục 1.2), rồi cộng dồn khoảng Elo đo được giữa các cấp kề nhau. Đây là thang so sánh giữa các bot của app, **không phải điểm FIDE**. Cặp có tỉ lệ thắng ngoài 20%–80% cho Elo rất không chắc (đánh dấu ~).

| Cấp | Mục tiêu | Ước lượng | Lệch |
|---|---|---|---|
| Dễ | 800 | 800 | (neo) |
| Trung bình | 1350 | ~1174 | -176 |
| Khá | 1700 | 1412 | -288 |
| Giỏi | 2100 | ~1736 | -364 |
| Cao thủ | 2300 | 1916 | -384 |
| Thách đấu | 2500 | 2034 | -466 |
| Siêu việt | 2800 | 2006 | -794 |

## Kết quả mới nhất từng cặp kề

| Cặp | Đợt | Số ván (cần) | Thắng/Hòa/Thua | Tỉ lệ điểm | Khoảng tin cậy 95% | Khoảng đạt | Khoảng Elo (mục tiêu) | Kết luận |
|---|---|---|---|---|---|---|---|---|
| Trung bình vs Dễ | r2 | 100 (400) | 90/0/10 | 90,0% | 84,1%–95,9% | 93,0%–98,0% | ~+374 (550) | Cần thêm ván, sơ bộ |
| Khá vs Trung bình | r2 | 100 (200) | 79/2/19 | 80,0% | 72,2%–87,8% | 81,0%–95,0% | +238 (350) | Cần thêm ván, sơ bộ |
| Giỏi vs Khá | r2 | 100 (200) | 87/0/13 | 87,0% | 80,4%–93,6% | 85,0%–96,0% | ~+325 (400) | Đạt, sơ bộ |
| Cao thủ vs Giỏi | r2 | 100 (200) | 74/0/26 | 74,0% | 65,4%–82,6% | 64,0%–88,0% | +180 (200) | Đạt, sơ bộ |
| Thách đấu vs Cao thủ | r2 | 100 (200) | 66/1/33 | 66,5% | 57,3%–75,7% | 64,0%–88,0% | +118 (200) | Đạt, sơ bộ |
| Siêu việt vs Thách đấu | r2 | 100 (200) | 46/0/54 | 46,0% | 36,2%–55,8% | 76,0%–93,0% | -28 (300) | Không đạt (hai cấp quá sát), sơ bộ |

"Sơ bộ" = chưa đủ số ván guide yêu cầu nên kết luận chỉ để tham khảo.

## Lịch sử các đợt đo

### Đợt g12-accept-2 (2026-11-28)

Nghiệm thu đợt 2: Wall Go và Quoridor (chạy lại phần g10 mất do lỗi commit), các cặp cờ vua / cờ tướng / caro sau khi chỉnh cấp Khá, Giỏi, Dễ; thử trọng số chess-t3.

| Cặp | Thời gian Siêu việt (PC) | Số ván | Thắng/Hòa/Thua | Tỉ lệ điểm | Đi trước / đi sau | Kết luận |
|---|---|---|---|---|---|---|
| Trung bình vs Dễ | 2000 ms | 408 | 378/5/25 | 93,3% | 0.95 / 0.91 | Đạt |
| Khá vs Trung bình | 2000 ms | 204 | 181/1/22 | 89,0% | 0.87 / 0.91 | Đạt |
| Giỏi vs Khá | 2000 ms | 204 | 161/3/40 | 79,7% | 0.76 / 0.83 | Cần thêm ván |
| Cao thủ vs Giỏi | 2000 ms | 204 | 166/2/36 | 81,9% | 0.85 / 0.78 | Đạt |
| Thách đấu vs Cao thủ | 2000 ms | 204 | 176/1/27 | 86,5% | 0.86 / 0.87 | Đạt |
| Siêu việt vs Thách đấu | 2000 ms | 216 | 194/0/22 | 89,8% | 0.88 / 0.92 | Đạt |

### Đợt r1 (2026-11-27)

Đợt 1, cấu hình 7 cấp ban đầu (commit 918e36c, trước khi chỉnh). Siêu việt chạy botMillis ÷ 8 trên PC. Wall Go cặp Siêu việt vs Thách đấu, cờ tướng các cặp trên và cờ vua chưa chạy (dừng sớm để chạy đợt 2). LƯU Ý: tiến trình đợt 1 không dừng hẳn và chạy chồng lên đợt 2, nên các dòng cờ tướng của đợt này đo khi CPU quá tải (Siêu việt bị thiệt).

| Cặp | Thời gian Siêu việt (PC) | Số ván | Thắng/Hòa/Thua | Tỉ lệ điểm | Đi trước / đi sau | Kết luận |
|---|---|---|---|---|---|---|
| Trung bình vs Dễ | 875 ms | 100 | 63/4/33 | 65,0% | 0.65 / 0.65 | Không đạt (hai cấp quá sát), sơ bộ |
| Khá vs Trung bình | 875 ms | 100 | 79/2/19 | 80,0% | 0.79 / 0.81 | Cần thêm ván, sơ bộ |
| Giỏi vs Khá | 875 ms | 100 | 87/0/13 | 87,0% | 0.86 / 0.88 | Đạt, sơ bộ |
| Cao thủ vs Giỏi | 875 ms | 100 | 74/0/26 | 74,0% | 0.66 / 0.82 | Đạt, sơ bộ |
| Thách đấu vs Cao thủ | 875 ms | 100 | 60/0/40 | 60,0% | 0.46 / 0.74 | Cần thêm ván, sơ bộ |

### Đợt r2 (2026-11-27)

Đợt 2, cấu hình đã chỉnh sau đợt 1 (Siêu việt nghĩ lâu hơn). Siêu việt chạy botMillis ÷ 4 trên PC (k = 4 giả định). LƯU Ý: cả đợt chạy chồng với tiến trình đợt 1 chưa dừng (22 tiến trình trên 12 luồng): các cấp giới hạn theo nút không bị ảnh hưởng, nhưng Siêu việt (giới hạn theo thời gian) chỉ được khoảng nửa sức tính, nên các cặp Siêu việt vs Thách đấu bị đánh giá thấp. Cờ tướng chưa xong, cờ vua chưa chạy (dừng để viết lại engine).

| Cặp | Thời gian Siêu việt (PC) | Số ván | Thắng/Hòa/Thua | Tỉ lệ điểm | Đi trước / đi sau | Kết luận |
|---|---|---|---|---|---|---|
| Trung bình vs Dễ | 1750 ms | 100 | 90/0/10 | 90,0% | 0.92 / 0.88 | Cần thêm ván, sơ bộ |
| Khá vs Trung bình | 1750 ms | 100 | 79/2/19 | 80,0% | 0.79 / 0.81 | Cần thêm ván, sơ bộ |
| Giỏi vs Khá | 1750 ms | 100 | 87/0/13 | 87,0% | 0.86 / 0.88 | Đạt, sơ bộ |
| Cao thủ vs Giỏi | 1750 ms | 100 | 74/0/26 | 74,0% | 0.66 / 0.82 | Đạt, sơ bộ |
| Thách đấu vs Cao thủ | 1750 ms | 100 | 66/1/33 | 66,5% | 0.59 / 0.74 | Đạt, sơ bộ |
| Siêu việt vs Thách đấu | 1750 ms | 100 | 46/0/54 | 46,0% | 0.42 / 0.50 | Không đạt (hai cấp quá sát), sơ bộ |

