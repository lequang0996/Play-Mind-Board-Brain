# Hồ sơ game: Cờ caro (bàn vô hạn)

Theo mẫu BOT_GUIDE mục 12.2. Sinh bởi `tools/bot-bench/make_profiles.py`; số liệu đo nằm ở `tools/bot-bench/results/`, báo cáo chi tiết ở [docs/bot-results](../bot-results/README.md).

| Mục | Nội dung |
|---|---|
| Luật chốt | 5 quân liên tiếp trở lên thắng, trừ khi cả hai đầu của chuỗi cực đại bị quân đối phương chặn. Bàn không giới hạn (bot tính trên cửa sổ 32×32 quanh các quân; ngoài cửa sổ coi là ô trống). Bốn có một đầu bị chặn đỡ được bằng hai cách: chiếm ô thành năm, hoặc chặn đầu kia để dãy năm bị chặn hai đầu. |
| Kiểm thử luật | thế viết tay trong SelfCheck.kt (caro vo han: ...) |
| botMillis (Siêu việt) | 6000 ms trên điện thoại (Catalog.kt) |
| Hệ số chậm điện thoại k | **chưa đo trên máy thật**; tạm giả định k = 4 (BotLevels.kt) |
| Lợi thế đi trước | rất lớn: báo cáo riêng tỉ lệ điểm khi đi trước / đi sau |
| Nguồn bot | tự làm (CaroEngine: bảng hình 11 ô theo luật chặn hai đầu, VCF xét mọi cách đỡ, phòng VCF, PVS lặp sâu). Rapfi GPL-3.0; engine Gomocup chơi luật khác. Tra cứu 27/11/2026. |
| Engine đo đối chiếu | không có engine đo đúng luật |
| Nguồn thế mở đầu | 3 quân đầu ngẫu nhiên trong ô 5×5 quanh gốc |
| Điều kiện kết thúc | không có hòa (bàn vô hạn); trần 1200 nửa nước trong bộ đo |
| win_in_1 / lose_in_1 | hoàn thành năm (winCells) / chặn bốn của đối phương (luôn chạy trước mọi thứ khác) |
| g theo vùng nút | chưa đo |
| Bộ thế chiến thuật / "không được sai" | chưa lập (chỉ có các thế trong SelfCheck.kt) |

## 7 cấp (bảng cấu hình duy nhất: core/.../BotLevels.kt)

| Cấp | Cấu hình |
|---|---|
| Dễ | độ sâu 1; nhiễu p = 0.50, δ = 20_000; VCF tắt |
| Trung bình | độ sâu 1; nhiễu p = 0.15, δ = 20_000; VCF tắt |
| Khá | độ sâu 1; nhiễu p = 0.20, δ = 800; VCF tới 3 |
| Giỏi | 250 nút; nhiễu p = 0.03, δ = 200; VCF tới 12 |
| Cao thủ | 1_000 nút; VCF tới 24 |
| Thách đấu | 4_000 nút; VCF tới 24 |
| Siêu việt | toàn lực, thời gian botMillis; VCF tới 24 |

## Kết quả 6 cặp kề (đo sơ bộ, CHƯA nghiệm thu)

| Cặp | Số ván | Thắng/Hòa/Thua | S | Khoảng tin cậy 95% | Khoảng đạt | Đi trước / đi sau | Kết luận |
|---|---|---|---|---|---|---|---|
| Trung bình vs Dễ | 100 | 90/0/10 | 0.900 | 0.84–0.96 | 93%–98% | 0.98 / 0.82 | thấp hơn khoảng (cấp quá sát nhau) |
| Khá vs Trung bình | 100 | 82/0/18 | 0.820 | 0.74–0.90 | 81%–95% | 0.90 / 0.74 | trong khoảng |
| Giỏi vs Khá | 100 | 79/0/21 | 0.790 | 0.71–0.87 | 85%–96% | 0.90 / 0.68 | thấp hơn khoảng (cấp quá sát nhau) |
| Cao thủ vs Giỏi | 100 | 59/0/41 | 0.590 | 0.49–0.69 | 64%–88% | 0.68 / 0.50 | thấp hơn khoảng (cấp quá sát nhau) |
| Thách đấu vs Cao thủ | 100 | 62/0/38 | 0.620 | 0.52–0.72 | 64%–88% | 0.76 / 0.48 | thấp hơn khoảng (cấp quá sát nhau) |
| Siêu việt vs Thách đấu | 100 | 58/0/42 | 0.580 | 0.48–0.68 | 76%–93% | 0.78 / 0.38 | thấp hơn khoảng (cấp quá sát nhau) |

Siêu việt đo trên máy phát triển ở thời gian botMillis ÷ 4 (mục 3.2, k giả định). Số ván mỗi cặp ít hơn yêu cầu nghiệm thu (200, cặp Trung bình vs Dễ 400).

## Còn thiếu để nghiệm thu (mục 11.1)

- Đo k và nút/giây trên điện thoại thật (máy mạnh và máy thấp nhất hỗ trợ); p99 thời gian nghĩ, RAM, nóng máy.
- Đủ số ván cho 6 cặp kề và 5 cặp cách hai cấp + 3 cặp smoke; bộ thế mở đầu lọc cân bằng.
- Bộ thế chiến thuật và bộ ≥ 30 thế "không được sai" chạy với cả 7 cấp; 1000 ván tự đấu trộn cấp.
- Các cặp chưa vào khoảng đạt: theo guide mục 11.3 phải tăng sức Siêu việt, không hạ chuẩn.
