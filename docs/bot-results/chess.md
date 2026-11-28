# Kết quả đo bot: Cờ vua

Sinh tự động bởi `tools/bot-bench/make_reports.py` từ `tools/bot-bench/results/`. Tiêu chí: [BOT_GUIDE](../BOT_GUIDE.md) mục 1.3 và 3.4.

## Tóm tắt

| Mục | Giá trị |
|---|---|
| Số đợt đo | 7 |
| Tổng số ván đã đấu | 2804 |
| Cặp kề đạt (kết quả mới nhất) | 5/6 |
| Nghiệm thu | Chưa đạt (cần đủ 6 cặp kề với đủ số ván, 5 cặp cách hai cấp và đo trên điện thoại thật) |

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
| Trung bình | 1350 | ~1380 | +30 |
| Khá | 1700 | ~1815 | +115 |
| Giỏi | 2100 | ~2084 | -16 |
| Cao thủ | 2300 | 2223 | -77 |
| Thách đấu | 2500 | 2350 | -150 |
| Siêu việt | 2800 | ~2614 | -186 |

## Kết quả mới nhất từng cặp kề

| Cặp | Đợt | Số ván (cần) | Thắng/Hòa/Thua | Tỉ lệ điểm | Khoảng tin cậy 95% | Khoảng đạt | Khoảng Elo (mục tiêu) | Kết luận |
|---|---|---|---|---|---|---|---|---|
| Trung bình vs Dễ | g11-accept-chess-xiangqi-caro | 408 (400) | 383/23/2 | 96,7% | 95,4%–98,0% | 93,0%–98,0% | ~+580 (550) | Đạt |
| Khá vs Trung bình | g11-accept-chess-xiangqi-caro | 204 (200) | 185/8/11 | 92,6% | 89,3%–96,0% | 81,0%–95,0% | ~+435 (350) | Đạt |
| Giỏi vs Khá | g11-accept-chess-xiangqi-caro | 204 (200) | 167/3/34 | 82,6% | 77,4%–87,7% | 85,0%–96,0% | ~+269 (400) | Cần thêm ván |
| Cao thủ vs Giỏi | g11-accept-chess-xiangqi-caro | 204 (200) | 132/18/54 | 69,1% | 63,1%–75,1% | 64,0%–88,0% | +139 (200) | Đạt |
| Thách đấu vs Cao thủ | g11-accept-chess-xiangqi-caro | 204 (200) | 131/14/59 | 67,6% | 61,5%–73,8% | 64,0%–88,0% | +127 (200) | Đạt |
| Siêu việt vs Thách đấu | g11-accept-chess-xiangqi-caro | 216 (200) | 160/35/21 | 82,2% | 77,8%–86,5% | 76,0%–93,0% | ~+264 (300) | Đạt |

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

