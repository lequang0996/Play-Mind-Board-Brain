# Kết quả đo bot: Cờ tướng

Sinh tự động bởi `tools/bot-bench/make_reports.py` từ `tools/bot-bench/results/`. Tiêu chí: [BOT_GUIDE](../BOT_GUIDE.md) mục 1.3 và 3.4.

## Tóm tắt

| Mục | Giá trị |
|---|---|
| Số đợt đo | 5 |
| Tổng số ván đã đấu | 4618 |
| Cặp kề đạt (kết quả mới nhất) | 5/6 |
| Nghiệm thu | Chưa đạt (cần đủ 6 cặp kề với đủ số ván, 5 cặp cách hai cấp và đo trên điện thoại thật) |

## Chỉ số thông minh (PM-Elo ước lượng)

Neo cấp Dễ = 800 theo thang quy ước của guide (mục 1.2), rồi cộng dồn khoảng Elo đo được giữa các cấp kề nhau. Đây là thang so sánh giữa các bot của app, **không phải điểm FIDE**. Cặp có tỉ lệ thắng ngoài 20%–80% cho Elo rất không chắc (đánh dấu ~).

| Cấp | Mục tiêu | Ước lượng | Lệch |
|---|---|---|---|
| Dễ | 800 | 800 | (neo) |
| Trung bình | 1350 | ~1721 | +371 |
| Khá | 1700 | ~2172 | +472 |
| Giỏi | 2100 | ~2564 | +464 |
| Cao thủ | 2300 | 2697 | +397 |
| Thách đấu | 2500 | 2874 | +374 |
| Siêu việt | 2800 | ~3154 | +354 |

## Kết quả mới nhất từng cặp kề

| Cặp | Đợt | Số ván (cần) | Thắng/Hòa/Thua | Tỉ lệ điểm | Khoảng tin cậy 95% | Khoảng đạt | Khoảng Elo (mục tiêu) | Kết luận |
|---|---|---|---|---|---|---|---|---|
| Trung bình vs Dễ | r2 | 100 (400) | 100/0/0 | 100,0% | 100,0%–100,0% | 93,0%–98,0% | ~+921 (550) | Không đạt (hai cấp quá xa), sơ bộ |
| Khá vs Trung bình | r2 | 100 (200) | 93/1/6 | 93,5% | 88,7%–98,3% | 81,0%–95,0% | ~+451 (350) | Đạt, sơ bộ |
| Giỏi vs Khá | g12-accept-2 | 204 (200) | 185/0/19 | 90,7% | 86,7%–94,7% | 85,0%–96,0% | ~+391 (400) | Đạt |
| Cao thủ vs Giỏi | g13-accept-3-chess-t3 | 204 (200) | 136/7/61 | 68,4% | 62,1%–74,7% | 64,0%–88,0% | +133 (200) | Đạt |
| Thách đấu vs Cao thủ | g13-accept-3-chess-t3 | 204 (200) | 140/20/44 | 73,5% | 67,9%–79,2% | 64,0%–88,0% | +176 (200) | Đạt |
| Siêu việt vs Thách đấu | g13-accept-3-chess-t3 | 216 (200) | 160/41/15 | 83,6% | 79,6%–87,6% | 76,0%–93,0% | ~+281 (300) | Đạt |

"Sơ bộ" = chưa đủ số ván guide yêu cầu nên kết luận chỉ để tham khảo.

## Lịch sử các đợt đo

### Đợt g11-accept-chess-xiangqi-caro (2026-11-28)

Nghiệm thu 7 cấp cờ vua, cờ tướng, caro (caro: nhiễu rộng cho Trung bình và Dễ) với cấu hình đã chốt trong BotLevels.kt (6 cặp kề, 5 cặp cách hai cấp). Cờ vua dùng trọng số chess-t2.

| Cặp | Thời gian Siêu việt (PC) | Số ván | Thắng/Hòa/Thua | Tỉ lệ điểm | Đi trước / đi sau | Kết luận |
|---|---|---|---|---|---|---|
| Trung bình vs Dễ | 2000 ms | 408 | 406/0/2 | 99,5% | 1.00 / 1.00 | Không đạt (hai cấp quá xa) |
| Khá vs Trung bình | 2000 ms | 204 | 177/3/24 | 87,5% | 0.89 / 0.86 | Đạt |
| Giỏi vs Khá | 2000 ms | 204 | 171/2/31 | 84,3% | 0.90 / 0.79 | Cần thêm ván |
| Cao thủ vs Giỏi | 2000 ms | 204 | 109/11/84 | 56,1% | 0.58 / 0.54 | Không đạt (hai cấp quá sát) |
| Thách đấu vs Cao thủ | 2000 ms | 204 | 137/13/54 | 70,3% | 0.73 / 0.68 | Đạt |
| Siêu việt vs Thách đấu | 2000 ms | 216 | 177/27/12 | 88,2% | 0.89 / 0.87 | Đạt |

### Đợt g12-accept-2 (2026-11-28)

Nghiệm thu đợt 2: Wall Go và Quoridor (chạy lại phần g10 mất do lỗi commit), các cặp cờ vua / cờ tướng / caro sau khi chỉnh cấp Khá, Giỏi, Dễ; thử trọng số chess-t3.

| Cặp | Thời gian Siêu việt (PC) | Số ván | Thắng/Hòa/Thua | Tỉ lệ điểm | Đi trước / đi sau | Kết luận |
|---|---|---|---|---|---|---|
| Trung bình vs Dễ | 2000 ms | 408 | 395/1/12 | 96,9% | 0.97 / 0.97 | Đạt |
| Khá vs Trung bình | 2000 ms | 204 | 177/2/25 | 87,3% | 0.85 / 0.89 | Đạt |
| Giỏi vs Khá | 2000 ms | 204 | 185/0/19 | 90,7% | 0.91 / 0.90 | Đạt |
| Cao thủ vs Giỏi | 2000 ms | 204 | 115/13/76 | 59,6% | 0.62 / 0.57 | Cần thêm ván |

### Đợt g13-accept-3-chess-t3 (2026-11-28)

Nghiệm thu đợt 3: cờ vua với trọng số chess-t3 (toàn bộ chuỗi cấp + Stockfish 2800/3000), cờ tướng (Thách đấu n16000, Cao thủ n3000), Wall Go (Khá), Quoridor (các cấp giữa).

| Cặp | Thời gian Siêu việt (PC) | Số ván | Thắng/Hòa/Thua | Tỉ lệ điểm | Đi trước / đi sau | Kết luận |
|---|---|---|---|---|---|---|
| Cao thủ vs Giỏi | 2000 ms | 204 | 136/7/61 | 68,4% | 0.74 / 0.63 | Đạt |
| Thách đấu vs Cao thủ | 2000 ms | 204 | 140/20/44 | 73,5% | 0.74 / 0.73 | Đạt |
| Siêu việt vs Thách đấu | 2000 ms | 216 | 160/41/15 | 83,6% | 0.84 / 0.83 | Đạt |

### Đợt r1 (2026-11-27)

Đợt 1, cấu hình 7 cấp ban đầu (commit 918e36c, trước khi chỉnh). Siêu việt chạy botMillis ÷ 8 trên PC. Wall Go cặp Siêu việt vs Thách đấu, cờ tướng các cặp trên và cờ vua chưa chạy (dừng sớm để chạy đợt 2). LƯU Ý: tiến trình đợt 1 không dừng hẳn và chạy chồng lên đợt 2, nên các dòng cờ tướng của đợt này đo khi CPU quá tải (Siêu việt bị thiệt).

| Cặp | Thời gian Siêu việt (PC) | Số ván | Thắng/Hòa/Thua | Tỉ lệ điểm | Đi trước / đi sau | Kết luận |
|---|---|---|---|---|---|---|
| Trung bình vs Dễ | 1125 ms | 100 | 100/0/0 | 100,0% | 1.00 / 1.00 | Không đạt (hai cấp quá xa), sơ bộ |
| Khá vs Trung bình | 1125 ms | 100 | 93/1/6 | 93,5% | 0.95 / 0.92 | Đạt, sơ bộ |

### Đợt r2 (2026-11-27)

Đợt 2, cấu hình đã chỉnh sau đợt 1 (Siêu việt nghĩ lâu hơn). Siêu việt chạy botMillis ÷ 4 trên PC (k = 4 giả định). LƯU Ý: cả đợt chạy chồng với tiến trình đợt 1 chưa dừng (22 tiến trình trên 12 luồng): các cấp giới hạn theo nút không bị ảnh hưởng, nhưng Siêu việt (giới hạn theo thời gian) chỉ được khoảng nửa sức tính, nên các cặp Siêu việt vs Thách đấu bị đánh giá thấp. Cờ tướng chưa xong, cờ vua chưa chạy (dừng để viết lại engine).

| Cặp | Thời gian Siêu việt (PC) | Số ván | Thắng/Hòa/Thua | Tỉ lệ điểm | Đi trước / đi sau | Kết luận |
|---|---|---|---|---|---|---|
| Trung bình vs Dễ | 2000 ms | 100 | 100/0/0 | 100,0% | 1.00 / 1.00 | Không đạt (hai cấp quá xa), sơ bộ |
| Khá vs Trung bình | 2000 ms | 100 | 93/1/6 | 93,5% | 0.95 / 0.92 | Đạt, sơ bộ |

