# Kết quả đo bot: Cờ vây

Sinh tự động bởi `tools/bot-bench/make_reports.py` từ `tools/bot-bench/results/`. Tiêu chí: [BOT_GUIDE](../BOT_GUIDE.md) mục 1.3 và 3.4.

## Tóm tắt

| Mục | Giá trị |
|---|---|
| Số đợt đo | 0 |
| Tổng số ván đã đấu | 0 |
| Cặp kề đạt (kết quả mới nhất) | 0/6 |
| Ghi chú | Chưa đo: KataGo chỉ chạy được trong bản build Android (CI / điện thoại thật). |
| Nghiệm thu | Chưa đạt (cần đủ 6 cặp kề với đủ số ván, 5 cặp cách hai cấp và đo trên điện thoại thật) |

## Chỉ số thông minh (PM-Elo ước lượng)

Neo cấp Dễ = 800 theo thang quy ước của guide (mục 1.2), rồi cộng dồn khoảng Elo đo được giữa các cấp kề nhau. Đây là thang so sánh giữa các bot của app, **không phải điểm FIDE**. Cặp có tỉ lệ thắng ngoài 20%–80% cho Elo rất không chắc (đánh dấu ~).

| Cấp | Mục tiêu | Ước lượng | Lệch |
|---|---|---|---|
| Dễ | 800 | 800 | (neo) |
| Trung bình | 1350 | chưa đo |  |
| Khá | 1700 | chưa đo |  |
| Giỏi | 2100 | chưa đo |  |
| Cao thủ | 2300 | chưa đo |  |
| Thách đấu | 2500 | chưa đo |  |
| Siêu việt | 2800 | chưa đo |  |

## Kết quả mới nhất từng cặp kề

| Cặp | Đợt | Số ván (cần) | Thắng/Hòa/Thua | Tỉ lệ điểm | Khoảng tin cậy 95% | Khoảng đạt | Khoảng Elo (mục tiêu) | Kết luận |
|---|---|---|---|---|---|---|---|---|
| Trung bình vs Dễ | | (400) | | | | 93,0%–98,0% | (550) | Chưa đo |
| Khá vs Trung bình | | (200) | | | | 81,0%–95,0% | (350) | Chưa đo |
| Giỏi vs Khá | | (200) | | | | 85,0%–96,0% | (400) | Chưa đo |
| Cao thủ vs Giỏi | | (200) | | | | 64,0%–88,0% | (200) | Chưa đo |
| Thách đấu vs Cao thủ | | (200) | | | | 64,0%–88,0% | (200) | Chưa đo |
| Siêu việt vs Thách đấu | | (200) | | | | 76,0%–93,0% | (300) | Chưa đo |

"Sơ bộ" = chưa đủ số ván guide yêu cầu nên kết luận chỉ để tham khảo.

## Lịch sử các đợt đo

Chưa có đợt đo nào.
