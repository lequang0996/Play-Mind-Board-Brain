# Hồ sơ game: Cờ vua

Theo mẫu BOT_GUIDE mục 12.2. Sinh bởi `tools/bot-bench/make_profiles.py`; số liệu đo nằm ở `tools/bot-bench/results/`, báo cáo chi tiết ở [docs/bot-results](../bot-results/README.md).

| Mục | Nội dung |
|---|---|
| Luật chốt | Luật FIDE: nhập thành, bắt tốt qua đường, phong cấp; hòa khi hết nước (pat), luật 50 nước, thiếu quân. Luật riêng của app: nước tạo lại thế đã có (cùng bên tới lượt) thì bên vừa đi THUA. |
| Kiểm thử luật | perft (thế ban đầu độ sâu 1-4, Kiwipete 1-3, pos3, pos4) + thế viết tay trong SelfCheck.kt |
| botMillis (Siêu việt) | 8000 ms trên điện thoại (Catalog.kt) |
| Hệ số chậm điện thoại k | **chưa đo trên máy thật**; tạm giả định k = 4 (BotLevels.kt) |
| Lợi thế đi trước | nhỏ; đấu cặp ván đổi màu |
| Nguồn bot | tự làm (ChessEngine: bitboard, PVS, Lazy SMP, LMR, null-move, SEE, singular, correction history; đánh giá tuyến tính tinh chỉnh Texel). Stockfish GPL-3.0 chỉ làm engine đo trên CI; các ứng viên MIT (Leorik, Zahak, Arasan, Caissa) chưa qua cổng kỹ thuật. Tra cứu 27/11/2026. |
| Engine đo đối chiếu | Stockfish UCI_Elo (thang CCRL) trên CI, dòng gauge của bot-bench (bench/Gauge.kt) |
| Nguồn thế mở đầu | 2-4 nước ngẫu nhiên có seed (bench/LevelMatch.kt), chưa lọc cân bằng bằng Siêu việt |
| Điều kiện kết thúc | lặp thế = thua bên lặp; 50 nước; trần 1200 nửa nước trong bộ đo |
| win_in_1 / lose_in_1 | chiếu hết trong 1 nước (fastWin) / nước để đối phương chiếu hết ngay, hoặc tự lặp thế (fastLosesNext) |
| g theo vùng nút | chưa đo |
| Bộ thế chiến thuật / "không được sai" | chưa lập (chỉ có các thế trong SelfCheck.kt) |

## 7 cấp (bảng cấu hình duy nhất: core/.../BotLevels.kt)

| Cấp | Cấu hình |
|---|---|
| Dễ | độ sâu 1; nhiễu p = 1.0, δ = 500 |
| Trung bình | độ sâu 1; nhiễu p = 0.60, δ = 200 |
| Khá | độ sâu 2; nhiễu p = 0.50, δ = 150 |
| Giỏi | 700 nút; nhiễu p = 0.05, δ = 20 |
| Cao thủ | 4_000 nút |
| Thách đấu | 15_000 nút |
| Siêu việt | toàn lực, thời gian botMillis |

## Kết quả 6 cặp kề (đo sơ bộ, CHƯA nghiệm thu)

| Cặp | Số ván | Thắng/Hòa/Thua | S | Khoảng tin cậy 95% | Khoảng đạt | Đi trước / đi sau | Kết luận |
|---|---|---|---|---|---|---|---|
| Trung bình vs Dễ | 408 | 391/15/2 | 0.977 | 0.96–0.99 | 93%–98% | 0.97 / 0.98 | trong khoảng |
| Khá vs Trung bình | 204 | 185/5/14 | 0.919 | 0.88–0.95 | 81%–95% | 0.91 / 0.93 | trong khoảng |
| Giỏi vs Khá | 204 | 187/2/15 | 0.922 | 0.89–0.96 | 85%–96% | 0.95 / 0.89 | trong khoảng |
| Cao thủ vs Giỏi | 204 | 139/14/51 | 0.716 | 0.66–0.78 | 64%–88% | 0.71 / 0.73 | trong khoảng |
| Thách đấu vs Cao thủ | 204 | 134/18/52 | 0.701 | 0.64–0.76 | 64%–88% | 0.74 / 0.66 | trong khoảng |
| Siêu việt vs Thách đấu | 216 | 165/30/21 | 0.833 | 0.79–0.88 | 76%–93% | 0.83 / 0.83 | trong khoảng |

Siêu việt đo trên máy phát triển ở thời gian botMillis ÷ 4 (mục 3.2, k giả định). Số ván mỗi cặp ít hơn yêu cầu nghiệm thu (200, cặp Trung bình vs Dễ 400).

## Còn thiếu để nghiệm thu (mục 11.1)

- Đo k và nút/giây trên điện thoại thật (máy mạnh và máy thấp nhất hỗ trợ); p99 thời gian nghĩ, RAM, nóng máy.
- Đủ số ván cho 6 cặp kề và 5 cặp cách hai cấp + 3 cặp smoke; bộ thế mở đầu lọc cân bằng.
- Bộ thế chiến thuật và bộ ≥ 30 thế "không được sai" chạy với cả 7 cấp; 1000 ván tự đấu trộn cấp.
- Các cặp chưa vào khoảng đạt: theo guide mục 11.3 phải tăng sức Siêu việt, không hạ chuẩn.
