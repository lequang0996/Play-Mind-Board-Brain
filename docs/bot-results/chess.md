# Kết quả đo bot: Cờ vua

Sinh tự động bởi `tools/bot-bench/make_reports.py` từ `tools/bot-bench/results/`. Tiêu chí: [BOT_GUIDE](../BOT_GUIDE.md) mục 1.3 và 3.4.

## Tóm tắt

| Mục | Giá trị |
|---|---|
| Số đợt đo | 10 |
| Tổng số ván đã đấu | 7688 |
| Cặp kề đạt (kết quả mới nhất) | 6/6 |
| Nghiệm thu | Đạt (cần đủ 6 cặp kề với đủ số ván, 5 cặp cách hai cấp và đo trên điện thoại thật) |

## Đối chiếu Stockfish (Elo so với engine ngoài)

Đợt `g9-chess-t2-gauge-chains` (2026-11-28). Siêu việt đấu Stockfish 17.1 ở chế độ giới hạn sức `UCI_Elo`, Stockfish 1 luồng, bot 3 luồng, mỗi bên 2000 ms/nước (máy CI; tương ứng botMillis trên điện thoại với hệ số chậm k giả định, guide mục 3.2). Luật lặp thế của app tính là hòa khi đo.

| Stockfish UCI_Elo | Số ván | Thắng/Hòa/Thua | Điểm | Elo suy ra |
|---|---|---|---|---|
| 2800 | 80 | 25/37/18 | 54,4% | 2830 |
| 3000 | 40 | 4/24/12 | 40,0% | 2931 |

**Elo ước lượng gộp: 2863** → **ĐẠT mục tiêu 2800+** (tiêu chí: điểm ≥ 50% trước Stockfish UCI_Elo ≥ 2800, ≥ 40 ván). Thang `UCI_Elo` của Stockfish neo theo CCRL, không phải điểm FIDE; đây là thước đo gần nhất có thể chạy tự động.

## Chỉ số thông minh (PM-Elo ước lượng)

Neo cấp Dễ = 800 theo thang quy ước của guide (mục 1.2), rồi cộng dồn khoảng Elo đo được giữa các cấp kề nhau. Đây là thang so sánh giữa các bot của app, **không phải điểm FIDE**. Cặp có tỉ lệ thắng ngoài 20%–80% cho Elo rất không chắc (đánh dấu ~).

| Cấp | Mục tiêu | Ước lượng | Lệch |
|---|---|---|---|
| Dễ | 800 | 800 | (neo) |
| Trung bình | 1350 | ~1415 | +65 |
| Khá | 1700 | ~1699 | -1 |
| Giỏi | 2100 | ~2122 | +22 |
| Cao thủ | 2300 | 2288 | -12 |
| Thách đấu | 2500 | 2414 | -86 |
| Siêu việt | 2800 | ~2659 | -141 |

## Kết quả mới nhất từng cặp kề

| Cặp | Đợt | Số ván (cần) | Thắng/Hòa/Thua | Tỉ lệ điểm | Khoảng tin cậy 95% | Khoảng đạt | Khoảng Elo (mục tiêu) | Kết luận |
|---|---|---|---|---|---|---|---|---|
| Trung bình vs Dễ | g16-chess-t4-accept | 408 (400) | 387/20/1 | 97,3% | 96,2%–98,5% | 93,0%–98,0% | ~+615 (550) | Đạt |
| Khá vs Trung bình | g16-chess-t4-accept | 204 (200) | 168/6/30 | 83,8% | 78,9%–88,7% | 81,0%–95,0% | ~+284 (350) | Đạt |
| Giỏi vs Khá | g16-chess-t4-accept | 204 (200) | 187/2/15 | 92,2% | 88,5%–95,8% | 85,0%–96,0% | ~+423 (400) | Đạt |
| Cao thủ vs Giỏi | g16-chess-t4-accept | 204 (200) | 135/25/44 | 72,3% | 66,6%–78,0% | 64,0%–88,0% | +166 (200) | Đạt |
| Thách đấu vs Cao thủ | g16-chess-t4-accept | 204 (200) | 125/25/54 | 67,4% | 61,4%–73,4% | 64,0%–88,0% | +126 (200) | Đạt |
| Siêu việt vs Thách đấu | g16-chess-t4-accept | 216 (200) | 159/30/27 | 80,6% | 75,9%–85,2% | 76,0%–93,0% | ~+245 (300) | Đạt |

"Sơ bộ" = chưa đủ số ván guide yêu cầu nên kết luận chỉ để tham khảo.

## Lịch sử các đợt đo

### Đợt g11-accept-chess-xiangqi-caro (2026-11-28)

Nghiệm thu 7 cấp cờ vua, cờ tướng, caro (caro: nhiễu rộng cho Trung bình và Dễ) với cấu hình đã chốt trong BotLevels.kt (6 cặp kề, 5 cặp cách hai cấp). Cờ vua dùng trọng số chess-t2.

| Cặp | Thời gian Siêu việt (PC) | Số ván | Thắng/Hòa/Thua | Tỉ lệ điểm | Đi trước / đi sau | Kết luận |
|---|---|---|---|---|---|---|
| Trung bình vs Dễ | 2000 ms | 408 | 383/23/2 | 96,7% | 0.96 / 0.97 | Đạt |
| Khá vs Trung bình | 2000 ms | 204 | 185/8/11 | 92,6% | 0.94 / 0.92 | Đạt |
| Giỏi vs Khá | 2000 ms | 204 | 167/3/34 | 82,6% | 0.83 / 0.82 | Cần thêm ván |
| Cao thủ vs Giỏi | 2000 ms | 204 | 132/18/54 | 69,1% | 0.69 / 0.69 | Đạt |
| Thách đấu vs Cao thủ | 2000 ms | 204 | 131/14/59 | 67,6% | 0.75 / 0.60 | Đạt |
| Siêu việt vs Thách đấu | 2000 ms | 216 | 160/35/21 | 82,2% | 0.80 / 0.84 | Đạt |

### Đợt g12-accept-2 (2026-11-28)

Nghiệm thu đợt 2: Wall Go và Quoridor (chạy lại phần g10 mất do lỗi commit), các cặp cờ vua / cờ tướng / caro sau khi chỉnh cấp Khá, Giỏi, Dễ; thử trọng số chess-t3.

| Cặp | Thời gian Siêu việt (PC) | Số ván | Thắng/Hòa/Thua | Tỉ lệ điểm | Đi trước / đi sau | Kết luận |
|---|---|---|---|---|---|---|
| Khá vs Trung bình | 2000 ms | 204 | 169/14/21 | 86,3% | 0.84 / 0.88 | Đạt |
| Giỏi vs Khá | 2000 ms | 204 | 190/5/9 | 94,4% | 0.95 / 0.94 | Đạt |

### Đợt g13-accept-3-chess-t3 (2026-11-28)

Nghiệm thu đợt 3: cờ vua với trọng số chess-t3 (toàn bộ chuỗi cấp + Stockfish 2800/3000), cờ tướng (Thách đấu n16000, Cao thủ n3000), Wall Go (Khá), Quoridor (các cấp giữa).

| Cặp | Thời gian Siêu việt (PC) | Số ván | Thắng/Hòa/Thua | Tỉ lệ điểm | Đi trước / đi sau | Kết luận |
|---|---|---|---|---|---|---|
| Trung bình vs Dễ | 2000 ms | 408 | 391/15/2 | 97,7% | 0.97 / 0.98 | Đạt |
| Khá vs Trung bình | 2000 ms | 204 | 185/5/14 | 91,9% | 0.91 / 0.93 | Đạt |
| Giỏi vs Khá | 2000 ms | 204 | 187/2/15 | 92,2% | 0.95 / 0.89 | Đạt |
| Cao thủ vs Giỏi | 2000 ms | 204 | 139/14/51 | 71,6% | 0.71 / 0.73 | Đạt |
| Thách đấu vs Cao thủ | 2000 ms | 204 | 134/18/52 | 70,1% | 0.74 / 0.66 | Đạt |
| Siêu việt vs Thách đấu | 2000 ms | 216 | 165/30/21 | 83,3% | 0.83 / 0.83 | Đạt |

### Đợt g16-chess-t4-accept (2026-11-28)

Nghiệm thu cờ vua với trọng số chess-t4 (+24 Elo so với chess-t3 ở g14): Stockfish 2800 / 3000 và toàn bộ chuỗi 7 cấp.

| Cặp | Thời gian Siêu việt (PC) | Số ván | Thắng/Hòa/Thua | Tỉ lệ điểm | Đi trước / đi sau | Kết luận |
|---|---|---|---|---|---|---|
| Trung bình vs Dễ | 2000 ms | 408 | 387/20/1 | 97,3% | 0.98 / 0.97 | Đạt |
| Khá vs Trung bình | 2000 ms | 204 | 168/6/30 | 83,8% | 0.80 / 0.88 | Đạt |
| Giỏi vs Khá | 2000 ms | 204 | 187/2/15 | 92,2% | 0.97 / 0.88 | Đạt |
| Cao thủ vs Giỏi | 2000 ms | 204 | 135/25/44 | 72,3% | 0.73 / 0.72 | Đạt |
| Thách đấu vs Cao thủ | 2000 ms | 204 | 125/25/54 | 67,4% | 0.69 / 0.66 | Đạt |
| Siêu việt vs Thách đấu | 2000 ms | 216 | 159/30/27 | 80,6% | 0.83 / 0.78 | Đạt |

