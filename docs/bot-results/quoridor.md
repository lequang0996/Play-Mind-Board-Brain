# Kết quả đo bot: Quoridor

Sinh tự động bởi `tools/bot-bench/make_reports.py` từ `tools/bot-bench/results/`. Tiêu chí: [BOT_GUIDE](../BOT_GUIDE.md) mục 1.3 và 3.4.

## Tóm tắt

| Mục | Giá trị |
|---|---|
| Số đợt đo | 6 |
| Tổng số ván đã đấu | 7758 |
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

### Đợt g12-accept-2 (2026-11-28)

Nghiệm thu đợt 2: Wall Go và Quoridor (chạy lại phần g10 mất do lỗi commit), các cặp cờ vua / cờ tướng / caro sau khi chỉnh cấp Khá, Giỏi, Dễ; thử trọng số chess-t3.

| Cặp | Thời gian Siêu việt (PC) | Số ván | Thắng/Hòa/Thua | Tỉ lệ điểm | Đi trước / đi sau | Kết luận |
|---|---|---|---|---|---|---|
| Trung bình vs Dễ | 2000 ms | 408 | 398/0/10 | 97,5% | 0.96 / 0.99 | Đạt |
| Khá vs Trung bình | 2000 ms | 204 | 127/0/77 | 62,3% | 0.56 / 0.69 | Không đạt (hai cấp quá sát) |
| Giỏi vs Khá | 2000 ms | 204 | 140/0/64 | 68,6% | 0.67 / 0.71 | Không đạt (hai cấp quá sát) |
| Cao thủ vs Giỏi | 2000 ms | 204 | 103/0/101 | 50,5% | 0.44 / 0.57 | Không đạt (hai cấp quá sát) |
| Thách đấu vs Cao thủ | 2000 ms | 204 | 137/0/67 | 67,2% | 0.68 / 0.67 | Đạt |
| Siêu việt vs Thách đấu | 2000 ms | 216 | 171/0/45 | 79,2% | 0.78 / 0.81 | Đạt |

### Đợt g13-accept-3-chess-t3 (2026-11-28)

Nghiệm thu đợt 3: cờ vua với trọng số chess-t3 (toàn bộ chuỗi cấp + Stockfish 2800/3000), cờ tướng (Thách đấu n16000, Cao thủ n3000), Wall Go (Khá), Quoridor (các cấp giữa).

| Cặp | Thời gian Siêu việt (PC) | Số ván | Thắng/Hòa/Thua | Tỉ lệ điểm | Đi trước / đi sau | Kết luận |
|---|---|---|---|---|---|---|
| Trung bình vs Dễ | 2000 ms | 408 | 345/0/63 | 84,6% | 0.75 / 0.94 | Không đạt (hai cấp quá sát) |
| Khá vs Trung bình | 2000 ms | 204 | 145/0/59 | 71,1% | 0.69 / 0.74 | Không đạt (hai cấp quá sát) |
| Giỏi vs Khá | 2000 ms | 204 | 116/0/88 | 56,9% | 0.49 / 0.65 | Không đạt (hai cấp quá sát) |
| Cao thủ vs Giỏi | 2000 ms | 204 | 127/0/77 | 62,3% | 0.62 / 0.63 | Cần thêm ván |

### Đợt g15-quoridor-levels-v2 (2026-11-28)

Quoridor: cấp mới dùng "quên tường" (extra = % nước chỉ đi quân) kết hợp nhiễu nhỏ trên nước đi quân; toàn bộ chuỗi 7 cấp. Wall Go Giỏi-Khá đo lại với nhiều ván hơn. Cờ tướng Cao thủ-Khá (cặp cách hai cấp sau khi đổi Cao thủ).

| Cặp | Thời gian Siêu việt (PC) | Số ván | Thắng/Hòa/Thua | Tỉ lệ điểm | Đi trước / đi sau | Kết luận |
|---|---|---|---|---|---|---|
| Trung bình vs Dễ | 2000 ms | 402 | 382/0/20 | 95,0% | 0.92 / 0.98 | Đạt |
| Khá vs Trung bình | 2000 ms | 204 | 172/0/32 | 84,3% | 0.86 / 0.82 | Đạt |
| Giỏi vs Khá | 2000 ms | 204 | 171/0/33 | 83,8% | 0.86 / 0.81 | Cần thêm ván |
| Cao thủ vs Giỏi | 2000 ms | 204 | 158/0/46 | 77,5% | 0.74 / 0.81 | Đạt |
| Thách đấu vs Cao thủ | 2000 ms | 204 | 157/0/47 | 77,0% | 0.78 / 0.75 | Đạt |
| Siêu việt vs Thách đấu | 2000 ms | 216 | 173/0/43 | 80,1% | 0.81 / 0.80 | Đạt |

### Đợt g17-quoridor-low-levels (2026-11-28)

Quoridor: Khá p0.5, Trung bình p0.75, Dễ p0.97 (sau g15: Giỏi-Khá 83,8%). Đo lại các cặp có ba cấp dưới.

| Cặp | Thời gian Siêu việt (PC) | Số ván | Thắng/Hòa/Thua | Tỉ lệ điểm | Đi trước / đi sau | Kết luận |
|---|---|---|---|---|---|---|
| Trung bình vs Dễ | 2000 ms | 402 | 386/0/16 | 96,0% | 0.94 / 0.99 | Đạt |
| Khá vs Trung bình | 2000 ms | 204 | 179/0/25 | 87,7% | 0.84 / 0.91 | Đạt |
| Giỏi vs Khá | 2000 ms | 204 | 189/0/15 | 92,6% | 0.93 / 0.92 | Đạt |

### Đợt r1 (2026-11-27)

Đợt 1, cấu hình 7 cấp ban đầu (commit 918e36c, trước khi chỉnh). Siêu việt chạy botMillis ÷ 8 trên PC. Wall Go cặp Siêu việt vs Thách đấu, cờ tướng các cặp trên và cờ vua chưa chạy (dừng sớm để chạy đợt 2). LƯU Ý: tiến trình đợt 1 không dừng hẳn và chạy chồng lên đợt 2, nên các dòng cờ tướng của đợt này đo khi CPU quá tải (Siêu việt bị thiệt).

| Cặp | Thời gian Siêu việt (PC) | Số ván | Thắng/Hòa/Thua | Tỉ lệ điểm | Đi trước / đi sau | Kết luận |
|---|---|---|---|---|---|---|
| Trung bình vs Dễ | 875 ms | 100 | 79/0/21 | 79,0% | 0.76 / 0.82 | Không đạt (hai cấp quá sát), sơ bộ |
| Khá vs Trung bình | 875 ms | 100 | 67/0/33 | 67,0% | 0.62 / 0.72 | Không đạt (hai cấp quá sát), sơ bộ |
| Giỏi vs Khá | 875 ms | 100 | 60/0/40 | 60,0% | 0.58 / 0.62 | Không đạt (hai cấp quá sát), sơ bộ |
| Cao thủ vs Giỏi | 875 ms | 100 | 73/0/27 | 73,0% | 0.78 / 0.68 | Đạt, sơ bộ |
| Thách đấu vs Cao thủ | 875 ms | 100 | 60/0/40 | 60,0% | 0.42 / 0.78 | Cần thêm ván, sơ bộ |
| Siêu việt vs Thách đấu | 875 ms | 100 | 58/0/42 | 58,0% | 0.48 / 0.68 | Không đạt (hai cấp quá sát), sơ bộ |

### Đợt r2 (2026-11-27)

Đợt 2, cấu hình đã chỉnh sau đợt 1 (Siêu việt nghĩ lâu hơn). Siêu việt chạy botMillis ÷ 4 trên PC (k = 4 giả định). LƯU Ý: cả đợt chạy chồng với tiến trình đợt 1 chưa dừng (22 tiến trình trên 12 luồng): các cấp giới hạn theo nút không bị ảnh hưởng, nhưng Siêu việt (giới hạn theo thời gian) chỉ được khoảng nửa sức tính, nên các cặp Siêu việt vs Thách đấu bị đánh giá thấp. Cờ tướng chưa xong, cờ vua chưa chạy (dừng để viết lại engine).

| Cặp | Thời gian Siêu việt (PC) | Số ván | Thắng/Hòa/Thua | Tỉ lệ điểm | Đi trước / đi sau | Kết luận |
|---|---|---|---|---|---|---|
| Trung bình vs Dễ | 1750 ms | 100 | 100/0/0 | 100,0% | 1.00 / 1.00 | Không đạt (hai cấp quá xa), sơ bộ |
| Khá vs Trung bình | 1750 ms | 100 | 62/0/38 | 62,0% | 0.54 / 0.70 | Không đạt (hai cấp quá sát), sơ bộ |
| Giỏi vs Khá | 1750 ms | 100 | 73/0/27 | 73,0% | 0.70 / 0.76 | Không đạt (hai cấp quá sát), sơ bộ |
| Cao thủ vs Giỏi | 1750 ms | 100 | 66/0/34 | 66,0% | 0.70 / 0.62 | Đạt, sơ bộ |
| Thách đấu vs Cao thủ | 1750 ms | 100 | 73/0/27 | 73,0% | 0.80 / 0.66 | Đạt, sơ bộ |
| Siêu việt vs Thách đấu | 1750 ms | 100 | 72/0/28 | 72,0% | 0.78 / 0.66 | Cần thêm ván, sơ bộ |

