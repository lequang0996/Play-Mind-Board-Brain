# Cẩm nang tạo bot cao thủ

Tài liệu này ghi lại cách bot của **Play Mind** được nâng từ mức nghiệp dư lên mức cao thủ, để các game sau (Reversi, Cờ đam, Connect 4, Cờ vây mini, Ô ăn quan...) làm theo cùng một quy trình. Mã nguồn tham chiếu nằm trong `core/src/main/kotlin/com/fws/playmind/core/`:

| Game | File | Điểm nhấn |
|---|---|---|
| Cờ vua | `Chess.kt` | PVS, bảng băm, null-move, LMR, SEE, đánh giá tapered, sách khai cuộc |
| Cờ tướng | `Xiangqi.kt` | Cùng khung, thêm đánh giá riêng (cung, sĩ tượng, pháo ghim tướng, tốt qua sông) |
| Cờ caro | `GomokuEngine.kt` (AI, bàn cố định) + `Gomoku.kt` (bàn vô hạn) | Đánh giá cửa sổ tăng dần, tìm kiếm chuỗi đe dọa VCF, phòng thủ trước VCF (VCT có sẵn nhưng tắt) |

## 1. Bốn trụ cột của một bot mạnh

1. **Luật đúng tuyệt đối.** Bot mạnh sẽ khai thác mọi lỗi luật. Kiểm chứng bằng *perft* (đếm số nút hợp lệ) trước khi viết một dòng AI.
2. **Tìm kiếm sâu và thông minh.** Alpha-beta chỉ là điểm xuất phát. Phần lớn sức mạnh đến từ *thứ tự duyệt nước đi* và *cắt tỉa/giảm độ sâu có kiểm soát*.
3. **Đánh giá có tri thức.** Vật chất + vị trí + cơ động + cấu trúc + an toàn + giai đoạn ván.
4. **Đo lường, không đoán.** Mỗi thay đổi phải thắng bản cũ trong trận tự đấu, và cả bản cuối phải được hiệu chỉnh với một engine chuẩn.

## 2. Quy trình cho một game đối kháng mới (checklist)

1. Cài đặt `BoardGame` (`Game.kt`): `play`, `legalMoves`, `copyGame`, `bestMove`, kết quả, lý do kết thúc. Nước đi mã hóa thành `Int`.
2. Viết `make/unmake` (đi và hoàn tác trên **một** bàn cờ, không sao chép bàn ở mỗi nút) kèm băm Zobrist cập nhật tăng dần.
3. Viết **perft** hoặc bộ kiểm thử luật tương đương và đối chiếu số chuẩn (cờ vua, cờ tướng có bảng số công khai). Game mới chưa có bảng chuẩn thì kiểm thử bằng các thế cờ viết tay + cho hai bản cài đặt độc lập đấu nhau.
4. Viết bản tìm kiếm cơ bản: `negamax` + alpha-beta + đào sâu dần + giới hạn thời gian.
5. Thêm từng kỹ thuật, **mỗi lần một kỹ thuật**, đo bằng trận A/B (xem mục 7).
6. Viết đánh giá, tinh chỉnh theo kết quả đo.
7. Thêm tri thức riêng của game (sách khai cuộc, tàn cuộc, tìm kiếm đe dọa...).
8. Đặt thời gian suy nghĩ cho điện thoại (mục 8), thêm kiểm thử vào `SelfCheck.kt` để CI bắt lỗi hồi quy.

## 3. Khung tìm kiếm (dùng cho mọi game hai người, thông tin đầy đủ)

```
searchRoot:  for depth = 1..N:                    // đào sâu dần
                 aspiration window quanh điểm vòng trước (±28)
                 sắp nước đi gốc: nước tốt nhất vòng trước lên đầu
                 PVS: nước đầu cửa sổ đầy, các nước sau cửa sổ rỗng rồi tìm lại nếu cần
                 dừng khi hết thời gian mềm (≈55% ngân sách) hoặc đã thấy chiếu hết

search(depth, alpha, beta):
    hết giờ -> abort
    hòa do lặp thế / luật 50 nước -> 0            // nhớ kiểm tra cả lịch sử ván, không chỉ trong cây tìm kiếm
    cắt tỉa khoảng cách chiếu hết
    bị chiếu -> depth++                            // mở rộng khi chiếu
    depth <= 0 -> quiesce()
    tra bảng băm: đủ sâu & cận hợp lệ -> trả luôn (không áp dụng ở nút PV)
    reverse futility: eval - m*depth >= beta -> trả eval
    null move:   (không bị chiếu, còn quân nặng, eval >= beta) đi "bỏ lượt" với độ sâu giảm R = 2 + depth/4
    IID:         không có nước từ bảng băm & depth >= 5 -> depth--
    sinh nước, chấm điểm sắp xếp, chọn dần (selection sort)
    với mỗi nước:
        futility / late-move pruning cho nước tĩnh ở độ sâu thấp (bỏ qua nếu nước đó chiếu hoặc đang bị chiếu)
        LMR: nước tĩnh, muộn, không chiếu -> giảm độ sâu theo bảng ln(depth)*ln(moveCount); tìm lại nếu vượt alpha
        PVS như trên
        cắt beta: cập nhật killer + history, thưởng nước gây cắt, phạt các nước tĩnh đã thử trước đó
    lưu bảng băm (cận trên / cận dưới / chính xác, kèm nước tốt nhất)

quiesce(alpha, beta):   // chỉ xét nước ăn quân (hoặc mọi nước nếu đang bị chiếu)
    stand-pat, delta pruning, bỏ nước ăn lỗ theo SEE
```

### Thứ tự duyệt nước đi (quan trọng nhất)

1. Nước từ bảng băm
2. Ăn quân có SEE ≥ 0, xếp theo `giá trị quân bị ăn × 16 − giá trị quân ăn`
3. Phong cấp lên hậu
4. Killer (2 nước/độ sâu)
5. Nước tĩnh theo bảng *history* (tăng khi gây cắt beta, giảm khi bị bỏ qua)
6. Ăn quân lỗ theo SEE (cuối cùng)

### Các tham số đã dùng (khởi điểm tốt, hãy tinh chỉnh bằng đo lường)

| Tham số | Cờ vua | Cờ tướng |
|---|---|---|
| Bảng băm | 2^19 mục (≈10 MB), khóa 64 bit | 2^19 mục |
| Null-move R | 2 + depth/4 + min(2, (eval−beta)/200) | 2 + depth/4 |
| Reverse futility | depth ≤ 6, 90·depth | depth ≤ 5, 110·depth |
| Futility | depth ≤ 3, 110·depth + 40 | depth ≤ 3, 120·depth + 60 |
| Late-move pruning | quiet thứ > 6 + depth² (depth ≤ 4) | quiet thứ > 8 + depth² |
| LMR | 0,7 + ln(d)·ln(m)/2,3 | 0,6 + ln(d)·ln(m)/2,4 |
| Aspiration | ±28, nới ×3 | ±30, nới ×3 |

## 4. Đánh giá (tapered) cho cờ vua

`điểm = (trungCuộc·giaiĐoạn + tànCuộc·(24 − giaiĐoạn)) / 24`, giai đoạn tính theo quân nặng còn lại.

* Vật chất theo hai bộ giá trị (trung cuộc / tàn cuộc) + bảng điểm vị trí (PST).
* **Cơ động** (đếm ô đi được) của mã, tượng, xe, hậu, trừ mức cơ sở.
* **Cấu trúc tốt:** tốt thông (thưởng theo hàng), tốt cô lập, tốt chồng.
* **Xe:** cột mở / nửa mở, hàng 7. **Cặp tượng.**
* **An toàn vua (trung cuộc):** lá chắn tốt, và phạt theo `(trọng số quân tấn công × số ô vùng vua bị tấn công)²`.
* **Tàn cuộc:** dồn vua đối phương ra mép khi hơn rõ rệt; chia điểm khi thế dễ hòa (không còn tốt, ưu thế nhỏ).

Cờ tướng áp dụng cùng ý tưởng với yếu tố riêng: giá trị mã/pháo đổi theo giai đoạn (pháo mạnh lúc đầu, mã mạnh lúc cuối), tốt qua sông được thưởng mạnh, **sĩ + tượng** có giá trị phòng thủ tăng theo lực tấn công của đối phương, thưởng pháo nhắm tướng qua một ngòi, phạt khi tướng bị nhiều quân chiếu vào cung.

## 5. Sách khai cuộc nhỏ

Một danh sách dòng khai cuộc (chuỗi nước dạng chữ). Khi lịch sử ván trùng với đầu một dòng, chọn ngẫu nhiên nước kế tiếp trong các dòng khớp. Mỗi nước vẫn được kiểm tra bằng `findMove` nên một dòng sai sẽ bị bỏ qua chứ không gây lỗi. Sách giúp bot đi nhanh, đa dạng và tránh sai lầm khai cuộc.

## 6. Cờ caro: bàn vô hạn và tìm kiếm chuỗi đe dọa

**Bàn cờ vô hạn.** `GomokuGame` (`Gomoku.kt`) không có mép bàn: quân lưu trong `HashMap`, nước đi là một số Int đóng gói tọa độ (`GomokuGame.pack(x, y)`, `px`, `py`; phạm vi ±16384 ô). Luật thắng: 5 quân trở lên, trừ khi cả hai đầu bị *quân đối phương* chặn. Thuật toán bên dưới (cửa sổ 5 ô, VCF, alpha-beta) nằm trong `GomokuEngine` - engine bàn vuông cỡ cố định (tối đa 24x24) đã được tinh chỉnh và đo A/B. Mỗi lần bot nghĩ, `GomokuGame.bestMove` cắt một **cửa sổ vuông** bao các quân đang có (lề 5 ô, cỡ 15..24), nạp vào `GomokuEngine.setPosition`, tìm nước rồi dịch tọa độ ngược lại. Khi quân rải rộng hơn 24 ô, cửa sổ được đặt vào cụm quân "nóng" nhất (trọng số theo số quân kề + hai nước vừa đi), quân ngoài cửa sổ bị bỏ qua. Các bài kiểm thử `caro vo han: ...` trong `SelfCheck.kt` bảo vệ phần này.

**Tìm kiếm chuỗi đe dọa (trong `GomokuEngine`).**

Caro là game *chiến thuật thuần túy*, người giỏi thắng bằng các chuỗi đe dọa nên bot phải có tìm kiếm đe dọa chuyên dụng:

1. **Cửa sổ 5 ô:** mỗi hàng/cột/đường chéo có nhiều cửa sổ 5 ô. Với mỗi cửa sổ lưu số quân đen và số quân trắng, điểm bên X = tổng `W[số quân X]` trên các cửa sổ không có quân đối phương. Cập nhật tăng dần khi đặt hoặc nhấc quân (mỗi ô nằm trong tối đa 20 cửa sổ), nên đánh giá tốn O(1). Mẫu "ba hở" tự động có điểm gấp ba "ba bị chặn" vì thuộc nhiều cửa sổ hơn.
2. **Đe dọa:** cửa sổ có 4 quân + 1 ô trống là "bốn" (thắng nếu không chặn); cửa sổ có 3 quân + 2 ô trống cho nước tạo "bốn"; nước tạo ≥ 2 cửa sổ 3-quân đi qua nó là "ba hở".
3. **VCF** (*victory by continuous fours*): chỉ đi các nước tạo "bốn". Đối phương bắt buộc chặn đúng một ô (hoặc thua ngay nếu có hai ô thắng). Đệ quy đến khi thắng. Rất rẻ nên chạy sâu 24 nước.
4. **VCT** (có sẵn trong mã nhưng **đang tắt**, cờ `USE_VCT`): thêm nước "ba hở". Đối phương có nhiều cách đỡ nên **mọi** cách đỡ đều phải thua; chỉ nhận một "ba" là đe dọa thật khi nó tạo được bốn-hở/bốn-kép. Đo A/B cho thấy bật VCT khiến bot thua bản không có VCT (8–12), nên chỉ bật lại khi cải tiến và đo lại.
5. **Phòng thủ:** nếu đối phương có VCF khi mình bỏ lượt, chỉ xét các nước sau đó VCF của họ thất bại (*safe moves*), rồi mới tìm kiếm thường trên tập đó.
6. **Alpha-beta lặp sâu** trên ≤ 12 ứng viên mỗi nút (sắp theo `lợi ích tấn công + 0,9·lợi ích phòng thủ`), có bảng băm, và gọi VCF nhanh ở mỗi nút để "nhìn thấy" các đòn kết thúc.

Thứ tự quyết định ở gốc: thắng ngay → chặn thắng ngay → VCF của mình → phòng thủ trước VCF của đối phương → alpha-beta (và VCT nếu được bật).

## 7. Cách đo độ mạnh (đừng tin cảm giác)

**a) Kiểm thử đúng đắn:** perft (cờ vua, cờ tướng), thế cờ chiếu hết/hòa, thế chiến thuật có đáp án. Đưa vào `core/src/test/.../SelfCheck.kt`, chạy `gradle :core:selfCheck` ở CI.

**b) A/B tự đấu:** giữ bản cũ (đổi tên lớp `ChessOld`), cho hai bên đấu luân phiên màu, mở đầu bằng 2 nước ngẫu nhiên để đa dạng, cùng thời gian mỗi nước. Chấp nhận thay đổi khi thắng rõ rệt (≥ 10 ván, càng nhiều càng tốt).

**c) Hiệu chỉnh với engine chuẩn:**

* Cờ vua: `sudo apt-get install stockfish`, bật `UCI_LimitStrength=true`, `UCI_Elo=<1320..3190>`, cho Stockfish chạy `go movetime 60`, giao thức UCI qua `ProcessBuilder`.
* Cờ tướng: Pikafish (giao thức UCCI/UCI tương tự).
* Caro: các engine Gomocup (giao thức Piskvork).

Khung trận đấu tối thiểu (Kotlin):

```kotlin
val sf = ProcessBuilder("/usr/games/stockfish").redirectErrorStream(true).start()
// gửi: uci / setoption name UCI_Elo value 2200 / position startpos moves e2e4 ... / go movetime 60
// đọc dòng "bestmove xxxx" -> findMove(from, to, promo)
```

Lưu ý khi chạy ở máy chủ/CI: chạy tiến trình nền bằng `setsid nohup ... &` (nếu không tiến trình bị dừng khi lệnh kết thúc), một CPU thì chạy tuần tự để không làm sai lệch thời gian.

### Kết quả đo của dự án

Đo trên máy chủ 1 CPU; điện thoại chậm hơn khoảng 2–3 lần. Mẫu nhỏ (6–20 ván) nên chỉ mang tính hướng dẫn.

| Game | Đối thủ đo | Kết quả (bản mới) |
|---|---|---|
| Cờ vua | Bản cũ của dự án (250 ms/nước, 10 ván) | **10 thắng – 0 hòa – 0 thua** |
| Cờ vua | Stockfish 16 giới hạn 1800 Elo (bot 500 ms, SF 60 ms) | 6 thắng – 0 – 0 |
| Cờ vua | Stockfish 16 giới hạn 2200 Elo | 3 thắng – 2 hòa – 1 thua |
| Cờ vua | Stockfish 16 giới hạn 2600 Elo | 3 thắng – 2 hòa – 1 thua |
| Cờ tướng | Bản cũ (250 ms/nước, 10 ván) | **6 thắng – 4 hòa – 0 thua** |
| Cờ tướng | Pikafish (mạng nơ-ron) giới hạn 1.500 nút | 0 thắng – 4 hòa – 2 thua |
| Cờ tướng | Pikafish giới hạn 8.000 nút | 0 – 1 – 5 |
| Cờ tướng | Pikafish giới hạn 40.000 nút | 0 – 0 – 6 |
| Cờ caro | Bản cũ, chỉ alpha-beta tăng dần (100 ms, 20 ván) | 13 – 1 – 6 |
| Cờ caro | Bản cũ, cấu hình chốt: VCF + phòng thủ VCF (100 ms, 20 ván) | **16 – 0 – 4** |
| Cờ caro | Cùng cấu hình nhưng bật VCT | 8 – 0 – 12 (tệ hơn, nên tắt) |

Tốc độ ở thời gian dùng trong app (máy chủ): cờ vua 4,5 s đạt độ sâu 12 (≈ 270 nghìn nút/s); cờ tướng 4,5 s đạt độ sâu 14 (≈ 500 nghìn nút/s); caro 3,5 s đạt độ sâu 12.

**Đọc kết quả cho đúng:**

* **Cờ vua** đủ mạnh để thắng hoặc hòa Stockfish bị giới hạn ở mức kiện tướng, tức mạnh hơn rõ rệt phần lớn người chơi, kể cả người chơi câu lạc bộ giỏi. Đây *không* phải mức Stockfish đầy đủ (≈ 3500 Elo).
* **Cờ tướng** ngang một engine mạng nơ-ron khi engine đó chỉ được tìm 1.500 nút, nhưng thua rõ khi nó được tìm nhiều hơn. Bot dùng đánh giá viết tay nên không thể sánh với mạng nơ-ron; muốn lên nữa cần mạng nơ-ron (NNUE) hoặc đánh giá được tinh chỉnh bằng hàng chục nghìn ván tự đấu.
* **Cờ caro** chưa có engine chuẩn để đối chiếu (Rapfi/Gomocup), nên chỉ có số đo so với bản cũ và các bài kiểm thử chiến thuật.
* Đừng ghi "ngang đại kiện tướng" vào mô tả cửa hàng khi chưa có số đo chứng minh.

### Bài học rút ra từ các lần đo (rất đáng tham khảo)

1. **Tính năng "thông minh hơn" có thể làm bot yếu đi.** Bản caro đầy đủ tính năng ban đầu thua bản cũ 0–6. Chỉ khi bật/tắt **từng** tính năng và đo A/B mới tìm ra thủ phạm (VCT bị tắt, cấu hình chốt thắng 16–4).
2. **Ngân sách thời gian là tài nguyên.** Các module đe dọa từng chiếm 45–60% thời gian mỗi nước nên alpha-beta chỉ còn nửa thời gian. Giới hạn mỗi module ≤ 10–20% và cho chúng thoát sớm khi không có quân đe dọa.
3. **Kiểm tra giờ phải đủ dày.** Nếu mỗi nút tốn nhiều micro-giây (như quét đe dọa), kiểm tra `nanoTime` mỗi 1024 nút sẽ làm bot vượt giờ hàng giây. Kiểm tra mỗi 16 nút ở những nút nặng.
4. **"Hết giờ" không bao giờ được hiểu là "an toàn".** Hàm tìm VCF của đối phương trả `false` khi hết giờ; phải theo dõi cờ "đã cạn ngân sách" riêng.
5. **Quét toàn bàn ở mỗi nút là kẻ giết hiệu năng.** Chuyển sang danh sách "cửa sổ hoạt động" cập nhật tăng dần giúp kiểm tra đe dọa nhanh hơn hàng chục lần.
6. **Mảng đệm dùng chung giữa hai hàm đệ quy lồng nhau sẽ bị ghi đè.** Mỗi hàm có mảng riêng theo độ sâu.
7. **Hòa do lặp thế cần "contempt":** điểm hòa hơi âm cho bên của bot (15 cp cờ vua, 30 cp cờ tướng) để bot không hài lòng với hòa khi thế ngang.
8. **Cẩn thận lệnh `pkill -f`:** nó khớp cả dòng lệnh của chính shell chạy nó và có thể giết luôn phiên làm việc. Neo mẫu (`pgrep -f "^java -jar x.jar"`).


## 8. Hiệu năng và tích hợp trên điện thoại

* **Thời gian:** cờ vua/cờ tướng 4,5 s, caro 3,5 s mỗi nước (`Catalog.botMillis`). Máy điện thoại chậm hơn máy chủ khoảng 2–3 lần nên bot nhìn nông hơn một chút so với số đo ở mục 7; muốn mạnh hơn thì tăng thời gian, không cần đổi thuật toán.
* **Luôn kiểm soát thời gian:** đọc `System.nanoTime()` mỗi 1024 nút; khi hết giờ đặt cờ `abort`, lưu lại nước tốt nhất của vòng đã hoàn thành.
* **Không cấp phát trong vòng lặp nóng:** mảng nước đi, undo stack, bảng sắp xếp đều cấp phát một lần theo độ sâu.
* **Bảng băm dùng chung** (companion object) nên bọc `synchronized`; kiểm tra nước lấy từ bảng băm có trong danh sách nước hợp lệ trước khi dùng nếu cần an toàn tuyệt đối (ở đây, nước bảng băm chỉ dùng để xếp thứ tự nên khóa va chạm không gây nước sai).
* **Chạy trên luồng nền** (`Dispatchers.Default`) với bản sao của bàn cờ (`copyGame()`), không chạm vào bàn cờ đang hiển thị. `SessionViewModel` đã làm sẵn.
* **Nhịp đi giống người:** `BotNames.humanDelay()` đảm bảo bot không trả lời tức thì khi tìm thấy nước sớm.

## 9. Game không phải đối kháng (Giải đố, Tính toán, Ghi nhớ)

Các chủ đề còn lại thường là game *một người chơi* hoặc đua điểm, "bot" ở đây là **bộ giải** hoặc **đối thủ tạo điểm**:

* **Giải đố (Sudoku, Nonogram, 2048...):** ràng buộc + quay lui có lan truyền (CSP), hoặc tìm kiếm A*/IDA* kèm heuristic. Bot đối thủ chỉ cần "giải" với độ trễ giống người.
* **Tính toán:** sinh đề theo độ khó rồi tính đáp án chuẩn; đối thủ mô phỏng thời gian trả lời theo phân phối.
* **Ghi nhớ:** đối thủ mô phỏng trí nhớ có giới hạn (xác suất nhớ giảm theo số lượt) để vẫn công bằng nhưng khó.
* **Đối kháng khác:** Reversi (alpha-beta + trọng số ô + cơ động, giải trọn vẹn khi còn ≤ 14 ô trống), Connect 4 (negamax + bitboard + bảng băm gần như giải được), Cờ đam (alpha-beta + đánh giá vật chất/vị trí + cơ sở dữ liệu tàn cuộc), Cờ vây (MCTS).

## 10. Lỗi thường gặp khi làm bot

| Triệu chứng | Nguyên nhân thường gặp |
|---|---|
| Bot đi nước ngu ở độ sâu cao | Lỗi `make/unmake` không đối xứng (hash, quyền nhập thành, vua lệch vị trí). Chạy perft và so băm tăng dần với băm tính lại |
| Bot "treo" hoặc quá giờ | Quên kiểm tra giờ trong `quiesce`, hoặc vòng aspiration không thoát |
| Bot chấp nhận hòa khi đang hơn | Điểm hòa = 0 nhưng đánh giá hơi âm; hoặc phát hiện lặp thế quá rộng |
| Bot mạnh lên rồi tụt sau một thay đổi | Không có kiểm thử A/B; hãy luôn giữ bản cũ để đấu |
| Kết quả không lặp lại | Bảng băm dùng chung giữa các ván; xóa hoặc đừng so sánh kết quả giữa các lần chạy |
