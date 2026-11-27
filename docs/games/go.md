# Hồ sơ game: Cờ vây 19×19

Theo mẫu BOT_GUIDE mục 12.2. Sinh bởi `tools/bot-bench/make_profiles.py`; số liệu đo nằm ở `tools/bot-bench/results/`, báo cáo chi tiết ở [docs/bot-results](../bot-results/README.md).

| Mục | Nội dung |
|---|---|
| Luật chốt | Luật diện tích kiểu Tromp-Taylor, không gỡ quân chết (phải bắt hết trước khi bỏ lượt), cấm tự sát, siêu ko theo vị trí, komi 7,5, hai lần bỏ lượt kết thúc. |
| Kiểm thử luật | thế viết tay + phát lại lịch sử trong SelfCheck.kt |
| botMillis (Siêu việt) | 15000 ms trên điện thoại (Catalog.kt) |
| Hệ số chậm điện thoại k | **chưa đo trên máy thật**; tạm giả định k = 4 (BotLevels.kt) |
| Lợi thế đi trước | komi 7,5 |
| Nguồn bot | engine nền KataGo v1.18.2 (mã MIT) + mạng g170-b6c96 và g170e-b10c128 (CC0), xem docs/licenses. Dự phòng: MCTS-RAVE tự viết của core khi không chạy được KataGo. |
| Engine đo đối chiếu | GNU Go (tools/bench/go_vs_gnugo), chưa chạy cho 7 cấp |
| Nguồn thế mở đầu | (chưa đo trên PC: không được chạy tệp KataGo tải về trên máy phát triển) |
| Điều kiện kết thúc | hai lần bỏ lượt; trần size² × 3 nửa nước |
| win_in_1 / lose_in_1 | không áp dụng (mục 8.2): thay bằng bất biến cứng không tự lấp mắt / không áp dụng; cấp Dễ/Trung bình lọc thêm nước tự atari |
| g theo vùng nút | chưa đo |
| Bộ thế chiến thuật / "không được sai" | chưa lập (chỉ có các thế trong SelfCheck.kt) |

## 7 cấp (bảng cấu hình duy nhất: core/.../BotLevels.kt)

Khi chạy KataGo (app/.../match/KataGo.kt):

| Cấp | Mạng | Lượt | Chọn nước |
|---|---|---|---|
| Dễ | b6 | 1 lần chạy mạng | lấy mẫu theo chính sách, nhiệt độ 1,6, lọc tự lấp mắt / tự atari |
| Trung bình | b6 | 1 lần chạy mạng | như trên, nhiệt độ 0,8 |
| Khá | b6 | 12 | nhiệt độ 0,5 |
| Giỏi | b10 | 40 | nhiệt độ 0,2 |
| Cao thủ | b10 | 160 | không nhiễu |
| Thách đấu | b10 | 480 | không nhiễu |
| Siêu việt | b10 | theo thời gian 15 s | không nhiễu |

Dự phòng MCTS khi không chạy được KataGo:

| Cấp | Cấu hình |
|---|---|
| Dễ | 8 lượt mô phỏng |
| Trung bình | 40 lượt mô phỏng |
| Khá | 200 lượt mô phỏng |
| Giỏi | 800 lượt mô phỏng |
| Cao thủ | 3_200 lượt mô phỏng |
| Thách đấu | 12_000 lượt mô phỏng |
| Siêu việt | toàn lực, thời gian botMillis |

## Kết quả 6 cặp kề (đo sơ bộ, CHƯA nghiệm thu)

Chưa đo.

## Còn thiếu để nghiệm thu (mục 11.1)

- Đo k và nút/giây trên điện thoại thật (máy mạnh và máy thấp nhất hỗ trợ); p99 thời gian nghĩ, RAM, nóng máy.
- Đủ số ván cho 6 cặp kề và 5 cặp cách hai cấp + 3 cặp smoke; bộ thế mở đầu lọc cân bằng.
- Bộ thế chiến thuật và bộ ≥ 30 thế "không được sai" chạy với cả 7 cấp; 1000 ván tự đấu trộn cấp.
- Các cặp chưa vào khoảng đạt: theo guide mục 11.3 phải tăng sức Siêu việt, không hạ chuẩn.
