# Hồ sơ game: Wall Go

Theo mẫu BOT_GUIDE mục 12.2. Sinh bởi `tools/bot-bench/make_profiles.py`; số liệu đo nằm ở `tools/bot-bench/results/`, báo cáo chi tiết ở [docs/bot-results](../bot-results/README.md).

| Mục | Nội dung |
|---|---|
| Luật chốt | Bàn 7×7, 2-4 người; đặt quân theo thứ tự rắn; mỗi lượt đi 0-2 ô rồi bắt buộc đặt tường ở ô vừa đứng; hết ván khi mọi vùng chỉ còn quân của tối đa một người; lãnh thổ = số ô vùng chỉ có quân mình; hòa điểm xét vùng lớn nhất. Tường chỉ tăng nên không thể lặp thế. |
| Kiểm thử luật | thế viết tay + phát lại lịch sử trong SelfCheck.kt |
| botMillis (Siêu việt) | 7000 ms trên điện thoại (Catalog.kt) |
| Hệ số chậm điện thoại k | **chưa đo trên máy thật**; tạm giả định k = 4 (BotLevels.kt) |
| Lợi thế đi trước | nhỏ; cặp ván đổi màu |
| Nguồn bot | tự làm (minimax alpha-beta lặp sâu dần, sắp nước theo đánh giá tĩnh; paranoid cho 3-4 người). Remake MIT có AI yếu; WallZero chưa rõ giấy phép. Tra cứu 27/11/2026. |
| Engine đo đối chiếu | không có |
| Nguồn thế mở đầu | 2 nước đặt quân ngẫu nhiên có seed |
| Điều kiện kết thúc | theo luật lãnh thổ |
| win_in_1 / lose_in_1 | nước kết thúc ván mà mình thắng / nước để người kế tiếp kết thúc ván mà mình không thắng |
| g theo vùng nút | chưa đo |
| Bộ thế chiến thuật / "không được sai" | chưa lập (chỉ có các thế trong SelfCheck.kt) |

## 7 cấp (bảng cấu hình duy nhất: core/.../BotLevels.kt)

| Cấp | Cấu hình |
|---|---|
| Dễ | độ sâu 1; nhiễu p = 0.75, δ = 300 |
| Trung bình | độ sâu 1; nhiễu p = 0.20, δ = 60 |
| Khá | độ sâu 2; nhiễu p = 0.08, δ = 30 |
| Giỏi | độ sâu 3 |
| Cao thủ | 5_000 nút |
| Thách đấu | 10_000 nút |
| Siêu việt | toàn lực, thời gian botMillis |

## Kết quả 6 cặp kề (đo sơ bộ, CHƯA nghiệm thu)

| Cặp | Số ván | Thắng/Hòa/Thua | S | Khoảng tin cậy 95% | Khoảng đạt | Đi trước / đi sau | Kết luận |
|---|---|---|---|---|---|---|---|
| Trung bình vs Dễ | 100 | 90/0/10 | 0.900 | 0.84–0.96 | 93%–98% | 0.92 / 0.88 | thấp hơn khoảng (cấp quá sát nhau) |
| Khá vs Trung bình | 100 | 79/2/19 | 0.800 | 0.72–0.88 | 81%–95% | 0.79 / 0.81 | thấp hơn khoảng (cấp quá sát nhau) |
| Giỏi vs Khá | 100 | 87/0/13 | 0.870 | 0.80–0.94 | 85%–96% | 0.86 / 0.88 | trong khoảng |
| Cao thủ vs Giỏi | 100 | 74/0/26 | 0.740 | 0.65–0.83 | 64%–88% | 0.66 / 0.82 | trong khoảng |
| Thách đấu vs Cao thủ | 100 | 66/1/33 | 0.665 | 0.57–0.76 | 64%–88% | 0.59 / 0.74 | trong khoảng |
| Siêu việt vs Thách đấu | 100 | 46/0/54 | 0.460 | 0.36–0.56 | 76%–93% | 0.42 / 0.50 | thấp hơn khoảng (cấp quá sát nhau) |

Siêu việt đo trên máy phát triển ở thời gian botMillis ÷ 4 (mục 3.2, k giả định). Số ván mỗi cặp ít hơn yêu cầu nghiệm thu (200, cặp Trung bình vs Dễ 400).

## Còn thiếu để nghiệm thu (mục 11.1)

- Đo k và nút/giây trên điện thoại thật (máy mạnh và máy thấp nhất hỗ trợ); p99 thời gian nghĩ, RAM, nóng máy.
- Đủ số ván cho 6 cặp kề và 5 cặp cách hai cấp + 3 cặp smoke; bộ thế mở đầu lọc cân bằng.
- Bộ thế chiến thuật và bộ ≥ 30 thế "không được sai" chạy với cả 7 cấp; 1000 ván tự đấu trộn cấp.
- Các cặp chưa vào khoảng đạt: theo guide mục 11.3 phải tăng sức Siêu việt, không hạ chuẩn.
