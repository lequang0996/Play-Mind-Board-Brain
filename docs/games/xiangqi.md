# Hồ sơ game: Cờ tướng

Theo mẫu BOT_GUIDE mục 12.2. Sinh bởi `tools/bot-bench/make_profiles.py`; số liệu đo nằm ở `tools/bot-bench/results/`, báo cáo chi tiết ở [docs/bot-results](../bot-results/README.md).

| Mục | Nội dung |
|---|---|
| Luật chốt | Luật cờ tướng chuẩn: tướng không đối mặt, pháo ăn qua ngòi, mã/tượng bị cản; bên hết nước đi thua; hòa sau 120 nửa nước không ăn quân. Luật riêng: tạo lại thế đã có thì bên vừa đi THUA (thay luật chiếu dài/đuổi dài). |
| Kiểm thử luật | perft thế ban đầu độ sâu 1-3 + thế viết tay trong SelfCheck.kt |
| botMillis (Siêu việt) | 8000 ms trên điện thoại (Catalog.kt) |
| Hệ số chậm điện thoại k | **chưa đo trên máy thật**; tạm giả định k = 4 (BotLevels.kt) |
| Lợi thế đi trước | nhỏ; cặp ván đổi màu |
| Nguồn bot | tự làm (XiangqiEngine: cùng khung tìm kiếm với cờ vua, đánh giá tuyến tính tinh chỉnh Texel, correction history). Pikafish GPL-3.0 + mạng có điều khoản riêng: chỉ làm engine đo trên CI. Tra cứu 27/11/2026. |
| Engine đo đối chiếu | Pikafish UCI_Elo trên CI, dòng gauge xiangqi của bot-bench (bench/Gauge.kt) |
| Nguồn thế mở đầu | 2-4 nước ngẫu nhiên có seed |
| Điều kiện kết thúc | lặp thế = thua; 120 nửa nước không ăn quân = hòa |
| win_in_1 / lose_in_1 | chiếu bí / làm đối phương hết nước trong 1 nước / nước để đối phương thắng ngay hoặc tự lặp thế |
| g theo vùng nút | chưa đo |
| Bộ thế chiến thuật / "không được sai" | chưa lập (chỉ có các thế trong SelfCheck.kt) |

## 7 cấp (bảng cấu hình duy nhất: core/.../BotLevels.kt)

| Cấp | Cấu hình |
|---|---|
| Dễ | độ sâu 1; nhiễu p = 0.50, δ = 180 |
| Trung bình | độ sâu 3; nhiễu p = 0.30, δ = 100 |
| Khá | độ sâu 5; nhiễu p = 0.15, δ = 50 |
| Giỏi | 20_000 nút; nhiễu p = 0.05, δ = 25 |
| Cao thủ | 60_000 nút |
| Thách đấu | 180_000 nút |
| Siêu việt | toàn lực, thời gian botMillis |

## Kết quả 6 cặp kề (đo sơ bộ, CHƯA nghiệm thu)

| Cặp | Số ván | Thắng/Hòa/Thua | S | Khoảng tin cậy 95% | Khoảng đạt | Đi trước / đi sau | Kết luận |
|---|---|---|---|---|---|---|---|
| Trung bình vs Dễ | 100 | 100/0/0 | 1.000 | 1.00–1.00 | 93%–98% | 1.00 / 1.00 | cao hơn khoảng |
| Khá vs Trung bình | 100 | 93/1/6 | 0.935 | 0.89–0.98 | 81%–95% | 0.95 / 0.92 | trong khoảng |
| Giỏi vs Khá | | | | | 85%–96% | | chưa đo |
| Cao thủ vs Giỏi | | | | | 64%–88% | | chưa đo |
| Thách đấu vs Cao thủ | | | | | 64%–88% | | chưa đo |
| Siêu việt vs Thách đấu | | | | | 76%–93% | | chưa đo |

Siêu việt đo trên máy phát triển ở thời gian botMillis ÷ 4 (mục 3.2, k giả định). Số ván mỗi cặp ít hơn yêu cầu nghiệm thu (200, cặp Trung bình vs Dễ 400).

## Còn thiếu để nghiệm thu (mục 11.1)

- Đo k và nút/giây trên điện thoại thật (máy mạnh và máy thấp nhất hỗ trợ); p99 thời gian nghĩ, RAM, nóng máy.
- Đủ số ván cho 6 cặp kề và 5 cặp cách hai cấp + 3 cặp smoke; bộ thế mở đầu lọc cân bằng.
- Bộ thế chiến thuật và bộ ≥ 30 thế "không được sai" chạy với cả 7 cấp; 1000 ván tự đấu trộn cấp.
- Các cặp chưa vào khoảng đạt: theo guide mục 11.3 phải tăng sức Siêu việt, không hạ chuẩn.
