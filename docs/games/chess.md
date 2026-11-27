# Hồ sơ game: Cờ vua

Theo mẫu BOT_GUIDE mục 12.2. Sinh bởi `tools/bot-bench/make_profiles.py`; số liệu đo nằm ở `tools/bot-bench/results/`, báo cáo chi tiết ở [docs/bot-results](../bot-results/README.md).

| Mục | Nội dung |
|---|---|
| Luật chốt | Luật FIDE: nhập thành, bắt tốt qua đường, phong cấp; hòa khi hết nước (pat), luật 50 nước, thiếu quân. Luật riêng của app: nước tạo lại thế đã có (cùng bên tới lượt) thì bên vừa đi THUA. |
| Kiểm thử luật | perft (thế ban đầu độ sâu 1-4, Kiwipete 1-3, pos3, pos4) + thế viết tay trong SelfCheck.kt |
| botMillis (Siêu việt) | 8000 ms trên điện thoại (Catalog.kt) |
| Hệ số chậm điện thoại k | **chưa đo trên máy thật**; tạm giả định k = 4 (BotLevels.kt) |
| Lợi thế đi trước | nhỏ; đấu cặp ván đổi màu |
| Nguồn bot | tự làm (alpha-beta PVS, bảng băm, LMR, null-move, SEE). Stockfish GPL-3.0 chỉ làm engine đo; các ứng viên MIT (Leorik, Zahak, Arasan, Caissa) chưa qua cổng kỹ thuật. Tra cứu 27/11/2026. |
| Engine đo đối chiếu | Stockfish UCI_Elo (tools/bot-bench/ChessMatch.kt), chưa chạy cho 7 cấp |
| Nguồn thế mở đầu | 2-4 nước ngẫu nhiên có seed (LevelMatch.kt), chưa lọc cân bằng bằng Siêu việt |
| Điều kiện kết thúc | lặp thế = thua bên lặp; 50 nước; trần 1200 nửa nước trong bộ đo |
| win_in_1 / lose_in_1 | chiếu hết trong 1 nước (fastWin) / nước để đối phương chiếu hết ngay, hoặc tự lặp thế (fastLosesNext) |
| g theo vùng nút | chưa đo |
| Bộ thế chiến thuật / "không được sai" | chưa lập (chỉ có các thế trong SelfCheck.kt) |

## 7 cấp (bảng cấu hình duy nhất: core/.../BotLevels.kt)

| Cấp | Cấu hình |
|---|---|
| Dễ | độ sâu 1; nhiễu p = 0.50, δ = 150 |
| Trung bình | độ sâu 3; nhiễu p = 0.30, δ = 80 |
| Khá | độ sâu 5; nhiễu p = 0.15, δ = 40 |
| Giỏi | 10_000 nút; nhiễu p = 0.05, δ = 20 |
| Cao thủ | 40_000 nút |
| Thách đấu | 120_000 nút |
| Siêu việt | toàn lực, thời gian botMillis |

## Kết quả 6 cặp kề (đo sơ bộ, CHƯA nghiệm thu)

Chưa đo.

## Còn thiếu để nghiệm thu (mục 11.1)

- Đo k và nút/giây trên điện thoại thật (máy mạnh và máy thấp nhất hỗ trợ); p99 thời gian nghĩ, RAM, nóng máy.
- Đủ số ván cho 6 cặp kề và 5 cặp cách hai cấp + 3 cặp smoke; bộ thế mở đầu lọc cân bằng.
- Bộ thế chiến thuật và bộ ≥ 30 thế "không được sai" chạy với cả 7 cấp; 1000 ván tự đấu trộn cấp.
- Các cặp chưa vào khoảng đạt: theo guide mục 11.3 phải tăng sức Siêu việt, không hạ chuẩn.
