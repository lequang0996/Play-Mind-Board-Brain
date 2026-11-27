# Hồ sơ game: Quoridor

Theo mẫu BOT_GUIDE mục 12.2. Sinh bởi `tools/bot-bench/make_profiles.py`; số liệu đo nằm ở `tools/bot-bench/results/`, báo cáo chi tiết ở [docs/bot-results](../bot-results/README.md).

| Mục | Nội dung |
|---|---|
| Luật chốt | Bàn 9×9; 2 người mỗi bên 10 tường, 4 người mỗi bên 5 tường; nhảy thẳng/chéo qua quân; tường không được chặn kín đường. Luật riêng: 2 người, đi quân tạo lại thế đã có thì THUA; 4 người, nước đó bị cấm. Quá 300 nửa nước: hòa. |
| Kiểm thử luật | thế viết tay + phát lại lịch sử trong SelfCheck.kt |
| botMillis (Siêu việt) | 7000 ms trên điện thoại (Catalog.kt) |
| Hệ số chậm điện thoại k | **chưa đo trên máy thật**; tạm giả định k = 4 (BotLevels.kt) |
| Lợi thế đi trước | có; cặp ván đổi màu |
| Nguồn bot | tự làm (alpha-beta PVS + bảng băm Zobrist + killer cho 2 người; paranoid cho 4 người). Không có engine chuẩn. Tra cứu 27/11/2026. |
| Engine đo đối chiếu | không có |
| Nguồn thế mở đầu | 2 nước đi quân ngẫu nhiên có seed |
| Điều kiện kết thúc | lặp thế = thua (2 người) / cấm (4 người); 300 nửa nước = hòa |
| win_in_1 / lose_in_1 | đi quân tới cạnh đích / nước để người kế tiếp đi quân tới đích ngay |
| g theo vùng nút | chưa đo |
| Bộ thế chiến thuật / "không được sai" | chưa lập (chỉ có các thế trong SelfCheck.kt) |

## 7 cấp (bảng cấu hình duy nhất: core/.../BotLevels.kt)

| Cấp | Cấu hình |
|---|---|
| Dễ | độ sâu 1; nhiễu p = 0.80, δ = 400 |
| Trung bình | độ sâu 2; nhiễu p = 0.25, δ = 70 |
| Khá | độ sâu 3; nhiễu p = 0.20, δ = 50 |
| Giỏi | 2_500 nút; nhiễu p = 0.04, δ = 15 |
| Cao thủ | 8_000 nút |
| Thách đấu | 25_000 nút |
| Siêu việt | toàn lực, thời gian botMillis |

## Kết quả 6 cặp kề (đo sơ bộ, CHƯA nghiệm thu)

| Cặp | Số ván | Thắng/Hòa/Thua | S | Khoảng tin cậy 95% | Khoảng đạt | Đi trước / đi sau | Kết luận |
|---|---|---|---|---|---|---|---|
| Trung bình vs Dễ | 100 | 100/0/0 | 1.000 | 1.00–1.00 | 93%–98% | 1.00 / 1.00 | cao hơn khoảng |
| Khá vs Trung bình | 100 | 62/0/38 | 0.620 | 0.52–0.72 | 81%–95% | 0.54 / 0.70 | thấp hơn khoảng (cấp quá sát nhau) |
| Giỏi vs Khá | 100 | 73/0/27 | 0.730 | 0.64–0.82 | 85%–96% | 0.70 / 0.76 | thấp hơn khoảng (cấp quá sát nhau) |
| Cao thủ vs Giỏi | 100 | 66/0/34 | 0.660 | 0.57–0.75 | 64%–88% | 0.70 / 0.62 | trong khoảng |
| Thách đấu vs Cao thủ | 100 | 73/0/27 | 0.730 | 0.64–0.82 | 64%–88% | 0.80 / 0.66 | trong khoảng |
| Siêu việt vs Thách đấu | 100 | 72/0/28 | 0.720 | 0.63–0.81 | 76%–93% | 0.78 / 0.66 | thấp hơn khoảng (cấp quá sát nhau) |

Siêu việt đo trên máy phát triển ở thời gian botMillis ÷ 4 (mục 3.2, k giả định). Số ván mỗi cặp ít hơn yêu cầu nghiệm thu (200, cặp Trung bình vs Dễ 400).

## Còn thiếu để nghiệm thu (mục 11.1)

- Đo k và nút/giây trên điện thoại thật (máy mạnh và máy thấp nhất hỗ trợ); p99 thời gian nghĩ, RAM, nóng máy.
- Đủ số ván cho 6 cặp kề và 5 cặp cách hai cấp + 3 cặp smoke; bộ thế mở đầu lọc cân bằng.
- Bộ thế chiến thuật và bộ ≥ 30 thế "không được sai" chạy với cả 7 cấp; 1000 ván tự đấu trộn cấp.
- Các cặp chưa vào khoảng đạt: theo guide mục 11.3 phải tăng sức Siêu việt, không hạ chuẩn.
