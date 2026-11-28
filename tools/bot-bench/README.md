# Bộ đo độ mạnh bot

Mọi phép đo bot chạy trên **GitHub Actions** (workflow `.github/workflows/bot-bench.yml`); máy phát triển không cần chạy trận đo dài.
Mã đo nằm trong `core/src/test/kotlin/com/fws/playmind/core/bench/` (điểm vào `BenchMain.kt`), dùng đúng engine của app.
Cách đọc kết quả, khoảng đạt từng cặp cấp và số ván cần thiết: xem [`docs/BOT_GUIDE.md`](../../docs/BOT_GUIDE.md) mục 2, 3 và 11.

## Gửi một đợt đo

1. Sửa `tools/bot-bench/request.txt`: đặt `tag=` (tên đợt, cũng là tên tệp kết quả), `note=` (mô tả) và các dòng yêu cầu bên dưới.
2. Commit rồi push lên `main`. Workflow tự chia việc ra nhiều máy (mỗi máy 4 vCPU), chạy, gom kết quả và **commit vào `main`**:
   * `tools/bot-bench/results/<tag>.txt`: từng ván (`R ...`) và dòng tổng kết của mỗi yêu cầu;
   * `docs/bot-results/` và `docs/games/`: báo cáo do `make_reports.py`, `make_profiles.py` sinh lại.
3. Đọc trạng thái: tab Actions, hoặc `https://api.github.com/repos/<chủ>/<repo>/actions/runs`.

Chỉ một đợt chạy và một đợt chờ tại một thời điểm (nhóm `bot-bench`). **Push đợt mới khi đợt trước còn chờ thì đợt chờ bị hủy**, nên đợi đợt trước bắt đầu chạy rồi mới push đợt tiếp.

## Các loại dòng trong `request.txt`

| Dòng | Ý nghĩa |
|---|---|
| `gauge chess elo=2800 games=80 ms=2000 sfms=2000 shards=4 threads=3` | Siêu việt đấu Stockfish giới hạn `UCI_Elo` (thang CCRL, không phải FIDE). Stockfish được biên dịch trên máy CI. |
| `gauge xiangqi nodes=200000 games=20 ms=2000 shards=2 threads=3` | Siêu việt cờ tướng đấu Pikafish giới hạn số nút mỗi nước (Pikafish không có `UCI_Elo`; tải bản phát hành mới nhất trên máy CI). Kết quả là dòng thử nghiệm `exp gauge`. |
| `ladder chess 6 5 pairs=100 ms=2000 shards=5 [players=2]` | Hai cấp đấu nhau theo cặp ván đổi màu (cấp 0 Dễ ... 6 Siêu việt); `ms` là thời gian mỗi nước của Siêu việt. |
| `ab chess new@qd240 new games=1000 ms=200 shards=6 [nodes=N]` | So hai biến thể engine Siêu việt (thử một thay đổi, mục 2.6). |

Mọi dòng thêm được `props=k1=v1;k2=v2` (thuộc tính hệ thống). Ví dụ `props=pm.level.chess.5=n15000;pm.level.chess.4=n4000`
để thử cấu hình cấp mà không sửa mã (cú pháp giá trị: `d<độ sâu>` hoặc `n<số nút>`, thêm `,p<xác suất nhiễu>,x<delta>,e<extra>`; xem `BotLevels.override`). Các mục không ghi giữ nguyên giá trị của cấp đó: muốn bỏ nhiễu phải ghi rõ `p0,x0`.
Dòng `ladder` có `props` và mọi dòng `ab` là **thử nghiệm**: kết quả ghi dòng `exp ...` riêng, không vào báo cáo nghiệm thu.

### Người chơi của dòng `ab`

| Game | Người chơi |
|---|---|
| chess | `new` (bản hiện tại, 1 luồng), `smpN` (N luồng), `ref` (trọng số khởi điểm chưa tinh chỉnh) |
| xiangqi | `new`, `smpN`, `ref`, `old` (engine cờ tướng trước XiangqiEngine) |
| caro, quoridor, wallgo | `new`, `old` (bản sao engine trước đợt nâng cấp 11/2026: `GomokuOld.kt`, `QuoridorOld.kt`, `WallGoOld.kt`) |

Sau tên người chơi có thể thêm `@spec` (nối nhiều mục bằng `+`):
* cờ vua: tham số tìm kiếm `SP` trong `ChessEngine.kt`, ví dụ `new@qd240+futd130`;
* cờ vua, cờ tướng: `w:<tag>` nạp trọng số từ `tools/tune/results/<tag>.txt` (thử trọng số Texel mới trước khi dán vào mã);
* cờ tướng: `corr<N>` (biên correction history);
* caro: tham số `CP` trong `CaroEngine.kt`, ví dụ `new@oppw11+beam6=16`;
* Quoridor: tham số `QP` trong `Quoridor.kt`, ví dụ `new@ww90+hist0`.

## Tinh chỉnh Texel (cờ vua, cờ tướng)

Sửa `tools/tune/request.txt` (`tag`, `game`, `games` mỗi tiến trình, `nodes` mỗi nước, `shards`, `iters`) rồi push lên `main`.
Workflow `.github/workflows/tune.yml` sinh dữ liệu tự đấu trên nhiều máy, tinh chỉnh và commit `tools/tune/results/<tag>.txt` (chuỗi trọng số) cùng `<tag>.log`.
Trọng số **không** tự vào engine: thử bằng `ab chess new@w:<tag> new ...`, thắng rõ thì dán chuỗi vào `ChessWeightsData.kt` / `XiangqiWeightsData.kt`.

## Chạy tại máy (tùy chọn)

Biên dịch module `core` (gồm mã kiểm thử) rồi gọi `com.fws.playmind.core.bench.BenchMainKt`:
```
java -Xss16m -cp <classpath của core test> com.fws.playmind.core.bench.BenchMainKt ab caro new old 20 200 1
java -Xss16m -cp <classpath> com.fws.playmind.core.bench.BenchMainKt -Dpm.level.chess.5=n15000 ladder chess 6 5 10 2000 1
```
Mỗi ván in `R <ván> <A đi trước 1/0> <điểm của A> <số nửa nước> <lý do>`, cuối cùng `TOTAL W D L`.
Không chạy Stockfish/Pikafish (GPL-3.0) tại máy hay trong app: chúng chỉ là engine đo trên CI.
