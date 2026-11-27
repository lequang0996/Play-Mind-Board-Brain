# Kết quả đo bot

Mỗi game có một trang ghi: đã đo bao nhiêu đợt, bao nhiêu ván, từng cặp cấp có đạt khoảng của [BOT_GUIDE](../BOT_GUIDE.md) không, và chỉ số thông minh (PM-Elo ước lượng) của 7 cấp.
Sinh tự động bởi `python tools/bot-bench/make_reports.py` từ `tools/bot-bench/results/*.txt` (mỗi tệp là một đợt đo). Không sửa tay.

| Game | Số đợt | Tổng ván | Cặp kề đã đo | Cặp kề đạt | Siêu việt (PM-Elo, mục tiêu 2800) | Siêu việt so với Stockfish |
|---|---|---|---|---|---|---|
| [Cờ vua](chess.md) | 4 | 648 | 0/6 | 0/6 | chưa đủ dữ liệu | 2791 (đạt 2800+) |
| [Cờ tướng](xiangqi.md) | 2 | 400 | 2/6 | 1/6 | chưa đủ dữ liệu | chưa đo |
| [Cờ caro](caro.md) | 2 | 1200 | 6/6 | 1/6 | ~1863 | chưa đo |
| [Quoridor](quoridor.md) | 2 | 1200 | 6/6 | 2/6 | ~2423 | chưa đo |
| [Wall Go](wallgo.md) | 2 | 1100 | 6/6 | 3/6 | ~2006 | chưa đo |
| [Cờ vây](go.md) | 0 | 0 | 0/6 | 0/6 | chưa đủ dữ liệu | chưa đo |

## Lưu ý

- Các đợt hiện tại đo trên máy phát triển, Siêu việt chạy `botMillis ÷ k` với k giả định (chưa đo trên điện thoại thật, guide mục 3.2).
- Mỗi cặp mới có 100 ván, ít hơn yêu cầu nghiệm thu (200, riêng Trung bình vs Dễ 400), nên kết luận là sơ bộ.
- Chưa đo 5 cặp cách hai cấp (guide mục 1.3) và chưa đối chiếu engine ngoài (mục 3.5).
- ~ = ước lượng thô vì có cặp thắng ngoài khoảng 20%–80%.
