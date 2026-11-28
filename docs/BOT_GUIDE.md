# Cẩm nang bot Play Mind: 7 cấp độ, mỗi cấp một bot

Mỗi game của Play Mind có **7 bot**, mỗi bot một cấp: **Dễ, Trung bình, Khá, Giỏi, Cao thủ, Thách đấu, Siêu việt**. Bot Siêu việt là bot mạnh nhất có thể làm; sáu bot còn lại là các phiên bản giảm sức có kiểm soát, để người chơi ở mọi trình độ đều có đối thủ vừa sức.

Tài liệu này trả lời năm câu hỏi: mỗi cấp mạnh đến đâu (mục 1), đo thế nào (mục 2 và 3), làm ra 7 bot thế nào (mục 4), từng game cụ thể ra sao (mục 5 đến 10), và khi nào một game được coi là xong (mục 11 đến 13). Kỹ thuật tìm kiếm và đánh giá nằm ở phụ lục A. Phần kỹ thuật cài đặt vẫn nằm trong mã nguồn `core/src/main/kotlin/com/fws/playmind/core/`.

## Mục lục

1. [Thang 7 cấp](#1-thang-7-cấp)
2. [Cách tính Elo và tỉ lệ điểm](#2-cách-tính-elo-và-tỉ-lệ-điểm)
3. [Quy trình đo](#3-quy-trình-đo)
4. [Cách tạo 7 bot cho một game](#4-cách-tạo-7-bot-cho-một-game)
5. [Cờ vua](#5-cờ-vua)
6. [Cờ tướng](#6-cờ-tướng)
7. [Cờ caro](#7-cờ-caro)
8. [Cờ vây 19×19](#8-cờ-vây)
9. [Quoridor](#9-quoridor)
10. [Wall Go](#10-wall-go)
11. [Nghiệm thu một game](#11-nghiệm-thu-một-game)
12. [Thêm game mới: hồ sơ game](#12-thêm-game-mới-hồ-sơ-game)
13. [Bảng theo dõi](#13-bảng-theo-dõi)
- [Phụ lục A: kỹ thuật nền](#phụ-lục-a-kỹ-thuật-nền-đã-dùng-trong-dự-án)

---

## 1. Thang 7 cấp

### 1.1. Mỗi cấp là một bot

| Cấp | Tên | Ý định thiết kế | Hành vi mong đợi của bot |
|---|---|---|---|
| 1 | **Dễ** | Phần lớn người chơi, nhất là người mới | Nhìn rất nông, hay bỏ sót đòn hai nước. Không đi nước vô lý; luôn tận dụng nước thắng ngay |
| 2 | **Trung bình** | Trên mức trung bình, người chơi có tập luyện | Tránh mất quân trực tiếp, còn sai khi thế phức tạp |
| 3 | **Khá** | Khoảng top 5% người chơi trực tuyến | Biết chiến thuật cơ bản; thỉnh thoảng sai ở thế khó |
| 4 | **Giỏi** | Người chơi giải khá mạnh, nhóm hiếm trong số người chơi | Ít sai chiến thuật, có kế hoạch ngắn hạn |
| 5 | **Cao thủ** | Cấp kiện tướng, chỉ một phần rất nhỏ | Chơi chắc, ít sai; người chơi thường rất khó thắng |
| 6 | **Thách đấu** | Cấp đại kiện tướng, vài nghìn người trên thế giới | Gần như không sai lầm chiến thuật |
| 7 | **Siêu việt** | Chỉ vài người ở mỗi thời điểm, thường không quá một bàn tay | Toàn lực: bot mạnh nhất của game, không giảm sức, không nhiễu |

Cột "Ý định thiết kế" mô tả cảm giác khi chơi, lấy theo vị trí tương đối của người chơi cờ. Đây là mục tiêu thiết kế, **không phải số đo**: dự án không đo bot bằng người chơi thật.

### 1.2. Độ mạnh tương đối giữa các bot

Độ mạnh của 7 bot được xác định bằng **khoảng cách Elo giữa chúng**, đo bằng trận đấu giữa các bot với nhau (mục 3). Khoảng cách lấy theo thang quy ước dưới đây, trong đó Siêu việt được đặt ở 2800:

| Cấp | PM-Elo quy ước | Cách Siêu việt |
|---|---|---|
| Dễ | ~ 800 | −2000 |
| Trung bình | ~ 1350 | −1450 |
| Khá | ~ 1700 | −1100 |
| Giỏi | ~ 2100 | −700 |
| Cao thủ | ~ 2300 | −500 |
| Thách đấu | ~ 2500 | −300 |
| Siêu việt | ~ 2800 | 0 |

**Đây là thang quy ước, không phải điểm FIDE hay Chess.com.** Các mốc được chọn gần với mốc danh hiệu quen thuộc của cờ vua (2300, 2500...) cho dễ hình dung, nhưng không đo bằng người chơi có điểm FIDE nên không đổi qua lại với điểm FIDE được. Các con số cho biết bot cấp này mạnh hơn bot cấp kia bao nhiêu, không khẳng định bot ngang người chơi nào. Con số tuyệt đối chỉ có nghĩa khi đối chiếu với engine ngoài (mục 3.5, tùy chọn).

**Lưu ý về neo.** Thang này neo ở Siêu việt (2800), mà Siêu việt là bot mạnh nhất *làm được*, không phải bot được chọn để ngang một mức người chơi cụ thể. Nếu Siêu việt thực tế mạnh hơn hoặc yếu hơn mức 2800 quy ước (nhất là cờ vây, Quoridor, Wall Go), cả sáu cấp dưới dịch theo, và cột "Ý định thiết kế" ở mục 1.1 không còn đúng nguyên văn. Vì vậy cột đó chỉ dùng để hình dung cảm giác khi chơi. Khi đối chiếu engine ngoài (mục 3.5) cho thấy lệch nhiều, sửa mô tả cho khớp số đo, không dùng mô tả để ép bot.

### 1.3. Khoảng đạt cho từng cặp cấp kề nhau

Vì Elo không đo chính xác khi một bên thắng quá nhiều (mục 2.1), mỗi cặp cấp kề nhau được kiểm bằng **tỉ lệ điểm của bot cấp cao** (hòa tính 0,5). Cặp đạt khi tỉ lệ nằm trong khoảng đạt, tương ứng khoảng cách Elo lệch không quá 100 thấp hơn hoặc 150 cao hơn mục tiêu.

| Cặp (cấp cao vs cấp thấp) | Khoảng Elo mục tiêu | Tỉ lệ điểm kỳ vọng | Khoảng đạt |
|---|---|---|---|
| Trung bình vs Dễ | 550 | 96% | **93% đến 98%** |
| Khá vs Trung bình | 350 | 88% | **81% đến 95%** |
| Giỏi vs Khá | 400 | 91% | **85% đến 96%** |
| Cao thủ vs Giỏi | 200 | 76% | **64% đến 88%** |
| Thách đấu vs Cao thủ | 200 | 76% | **64% đến 88%** |
| Siêu việt vs Thách đấu | 300 | 85% | **76% đến 93%** |

Cặp Trung bình vs Dễ có khoảng đạt hẹp (5 điểm phần trăm) nên cần **400 ván** thay vì 200 (mục 2.2 và 3.4).

Ngoài ra có **kiểm tra khoảng cách tối thiểu** giữa các cấp cách nhau hai bậc (mục 3.4). Ngưỡng suy từ trường hợp xấu nhất: hai cặp kề cùng chạm biên dưới của khoảng đạt. Ví dụ Cao thủ vs Giỏi và Thách đấu vs Cao thủ cùng đạt 64% thì Thách đấu vs Giỏi chỉ còn khoảng 76%, nên ngưỡng cho cặp này không thể đặt cao hơn mức đó.

| Cặp cách hai cấp | Elo mục tiêu | Điểm kỳ vọng | Xấu nhất (hai cặp kề cùng chạm biên dưới) | Ngưỡng tối thiểu |
|---|---|---|---|---|
| Khá vs Dễ | 900 | 99,4% | 98,3% | **≥ 97%** |
| Giỏi vs Trung bình | 750 | 98,7% | 96,0% | **≥ 94%** |
| Cao thủ vs Khá | 600 | 96,9% | 91,0% | **≥ 88%** |
| Thách đấu vs Giỏi | 400 | 90,9% | 76,0% | **≥ 70%** |
| Siêu việt vs Cao thủ | 500 | 94,7% | 84,9% | **≥ 80%** |

Ngưỡng tối thiểu = điểm kỳ vọng ứng với khoảng Elo mục tiêu trừ 250 (làm tròn xuống). **Nếu Elo cộng dồn được** thì cả 6 cặp kề đạt kéo theo 5 cặp này đạt. Bot thường gần như vậy nhưng không bảo đảm: có bot thắng đều cấp kề dưới mà lại kém hiệu quả trước cấp cách hai bậc (bất bắc cầu, ví dụ một cấp có điểm yếu riêng mà chỉ cấp xa mới khai thác được). Vì vậy bước này bắt ba loại lỗi mà 6 cặp kề không thấy: bất bắc cầu, đảo cấp và lỗi cấu hình (tráo dòng, nhầm giới hạn). Nó không thay cho kiểm 6 cặp kề.

---

## 2. Cách tính Elo và tỉ lệ điểm

### 2.1. Công thức cơ bản

Gọi `S` là tỉ lệ điểm của một bot trong một trận (thắng = 1, hòa = 0,5, thua = 0):

```
S  = (thắng + 0,5 × hòa) / tổng số ván
Δ  = 400 × log10( S / (1 − S) )          // bot hơn đối thủ Δ điểm Elo
Kỳ vọng:  E = 1 / (1 + 10^(−Δ/400))
```

Mọi chỗ ghi "tỉ lệ thắng" trong tài liệu này đều là `S` ở trên (hòa tính 0,5), **không** phải `thắng / tổng số ván`; mã đo phải tính đúng như vậy.

Một số mốc để nhẩm: `S = 0,50` → Δ = 0; `S = 0,64` → +100; `S = 0,76` → +200; `S = 0,85` → +300; `S = 0,91` → +400; `S = 0,96` → +550.

**Điều kiện dùng công thức:** `S` nằm trong khoảng 20% đến 80%. Ngoài khoảng này, Elo tính ra không chính xác (một ván thắng thêm hay thua thêm làm Δ nhảy rất lớn). Đây là lý do mục 1.3 kiểm các cặp cấp bằng **khoảng tỉ lệ điểm**, không bằng Elo.

### 2.2. Số ván cần thiết

Sai số 95% của Elo xấp xỉ (khi `S ≈ 0,5`, ít hòa):

| Số ván | Sai số ±Elo (xấp xỉ) |
|---|---|
| 100 | ±68 |
| 200 | ±48 |
| 400 | ±34 |
| 1000 | ±21 |

Game nhiều hòa (cờ vua, cờ tướng) có độ phân tán nhỏ hơn nên sai số thấp hơn một chút. Dưới 100 ván thì kết quả chỉ mang tính tham khảo. Tỉ lệ điểm gần 0% hay 100% thì sai số tuyệt đối của tỉ lệ nhỏ đi, nhưng Elo tương ứng vẫn rất không chắc, nên không ước lượng Elo ở vùng đó.

Quy ước số ván của tài liệu này: **100 ván cho một vòng chỉnh, 200 ván cho một lần xác nhận, 400 ván cho cặp Trung bình vs Dễ.**

Cách chọn `n` cho một cặp: gọi `h` là nửa độ rộng khoảng đạt (tính theo điểm, ví dụ 0,025) và `S` là điểm kỳ vọng; cần `1,96 × √(S(1−S)/n) ≤ 0,8 × h`, tức `n ≥ (1,96 / (0,8·h))² × S(1−S)`. Cặp Trung bình vs Dễ (`h` = 0,025, `S` ≈ 0,96) ra khoảng 370, làm tròn 400; các cặp kề còn lại ra dưới 200. Với 200 ván cặp Trung bình vs Dễ, điểm đo của bot chỉnh đúng mục tiêu (96%) vẫn rơi ra ngoài khoảng đạt khoảng 9% số lần chỉ vì may rủi; với 400 ván còn khoảng 2%.

Các xác suất trên là cho **quy tắc quyết theo điểm đo** mà mục 3.4 dùng. Tài liệu cố ý không dùng quy tắc "cả khoảng tin cậy 95% phải nằm trọn trong khoảng đạt": với quy tắc đó, bot chỉnh đúng 96% đấu 400 ván chỉ đạt ngay khoảng 35% số lần, phần còn lại phải chạy thêm ván mãi. Với quy tắc của mục 3.4 (tối đa một đợt chạy thêm), tỉ lệ đạt của bot đó là khoảng 99,9% (mô phỏng 20.000 lần).

### 2.3. Mã tính tỉ lệ điểm và kiểm khoảng đạt

```python
import math

def elo_from_score(s):
    """Elo tương đối từ điểm S. Trả None khi S ngoài [0.2, 0.8] (mục 2.1)."""
    if not (0.2 <= s <= 0.8):
        return None
    return 400 * math.log10(s / (1 - s))

def score_ci(w, d, l, z=1.96):
    """Trả về (S, cận_dưới, cận_trên) của điểm S (thắng = 1, hòa = 0,5, thua = 0)."""
    n = w + d + l
    s = (w + 0.5 * d) / n
    var = (w * (1 - s) ** 2 + d * (0.5 - s) ** 2 + l * s ** 2) / (n - 1)   # phương sai mẫu mỗi ván
    se = math.sqrt(var / n)
    return s, s - z * se, s + z * se

def match_stats(w, d, l):
    """Trả về (S, elo, elo_dưới, elo_trên). Giá trị Elo là None nếu S hoặc cận ngoài [0.2, 0.8]."""
    s, lo, hi = score_ci(w, d, l)
    return s, elo_from_score(s), elo_from_score(lo), elo_from_score(hi)

def pair_verdict(w, d, l, lo, hi, merged=False):
    """Quy tắc cặp kề (mục 3.4), tối đa hai lần xem kết quả.
    S trong [lo, hi] -> 'đạt'. Khoảng tin cậy 95% không giao [lo, hi] -> 'không đạt'.
    Còn lại: lần đầu -> 'cần thêm ván' (chạy thêm đúng một đợt rồi gọi lại với merged=True);
    đã gộp -> 'không đạt'."""
    s, ci_lo, ci_hi = score_ci(w, d, l)
    if lo <= s <= hi:
        return "đạt"
    if ci_hi < lo or ci_lo > hi or merged:
        return "không đạt"
    return "cần thêm ván"

def min_gap_verdict(w, d, l, floor, merged=False):
    """Quy tắc cặp cách hai cấp (mục 3.4): điểm của cấp cao phải >= floor."""
    s, _, ci_hi = score_ci(w, d, l)
    if s >= floor:
        return "đạt"
    if ci_hi < floor or merged:
        return "không đạt"
    return "cần thêm ván"

# Ví dụ: Cao thủ vs Giỏi, 200 ván: 120 thắng, 30 hòa, 50 thua; khoảng đạt 64% đến 88%
print(pair_verdict(120, 30, 50, 0.64, 0.88))   # S = 0,675 nằm trong khoảng → "đạt"
# Ví dụ: 200 ván, 115 thắng, 20 hòa, 65 thua: S = 0,625 < 0,64 nhưng khoảng tin cậy ≈ 0,56 đến 0,69 còn chạm → "cần thêm ván"
print(pair_verdict(115, 20, 65, 0.64, 0.88))
```

### 2.4. Vòng tròn giữa nhiều bot

Khi muốn có một bảng Elo nhất quán cho cả 7 bot, đưa toàn bộ kết quả các cặp đã đấu vào một công cụ ước lượng cực đại hợp lý như **Ordo** hoặc **BayesElo**. 11 cặp của mục 3.4 (6 cặp kề và 5 cặp cách hai cấp) đã nối đủ 7 bot nên không bắt buộc chạy đủ 21 cặp; chỉ thêm cặp khi cần bảng chắc hơn. Cặp thắng gần 100% cho rất ít thông tin về Elo, nên khoảng sai số của các cấp ở hai đầu thang sẽ rộng. Đừng cộng dồn các Δ từng cặp bằng tay. Bảng này dùng để **báo cáo**, còn quyết định đạt hay không dựa trên các khoảng ở mục 1.3. Với game không có PGN, ghi kết quả dạng `bot_A bot_B kết_quả` rồi chuyển sang định dạng mà công cụ chấp nhận.

### 2.5. Lợi thế đi trước

Game có lợi thế đi trước lớn (caro, Quoridor) làm sai lệch kết quả nếu một bot cầm một màu nhiều hơn.

* Luôn đấu **cặp ván**: cùng một thế mở đầu, hai bên đổi màu.
* Báo cáo thêm tỉ lệ điểm khi đi trước và khi đi sau riêng, vì chênh lệch lớn là dấu hiệu bot yếu ở một vai.
* Dùng luật hoán đổi (swap) hoặc thế mở đầu cân bằng khi game hỗ trợ.

### 2.6. SPRT: chấp nhận một thay đổi

Khi sửa bot (nhất là bot Siêu việt), không đợi đủ số ván cố định mà dùng kiểm định **SPRT** để dừng sớm khi đã đủ bằng chứng.

```python
import math

def _score(elo): return 1 / (1 + 10 ** (-elo / 400))

def sprt_llr(w, d, l, elo0=0.0, elo1=5.0):
    """Log-likelihood ratio (xấp xỉ chuẩn). H0: chênh = elo0, H1: chênh = elo1."""
    n = w + d + l
    if n == 0: return 0.0
    s = (w + 0.5 * d) / n
    var = (w * (1 - s) ** 2 + d * (0.5 - s) ** 2 + l * s ** 2) / n
    var = max(var, 1e-3)    # tránh chia 0 khi chuỗi toàn thắng hoặc toàn thua
    s0, s1 = _score(elo0), _score(elo1)
    return n * (s1 - s0) * (2 * s - s0 - s1) / (2 * var)

UPPER = math.log(0.95 / 0.05)    # ≈ +2,94: chấp nhận H1 (bản mới mạnh hơn)
LOWER = math.log(0.05 / 0.95)    # ≈ −2,94: chấp nhận H0 (loại bản mới)
```

Chạy từng cặp ván cho đến khi `LLR ≥ UPPER` (nhận) hoặc `LLR ≤ LOWER` (loại). Mỗi thay đổi chỉ được gộp khi qua SPRT; điều này chặn đúng lỗi "tính năng thông minh hơn làm bot yếu đi" ở phụ lục A.

Hai điều chỉnh thực tế: (1) đặt **trần số ván** (đặt riêng cho từng game và ghi trong hồ sơ game; game ván dài như cờ vây thấp hơn nhiều, nên cờ vây thường chỉ dùng SPRT ở ngân sách rút gọn) và coi hết trần mà chưa nhận là loại; (2) với game ván dài, chọn `elo1` lớn hơn (10 đến 20) vì `elo1 = 5` thường cần hàng chục nghìn ván. Nếu công cụ chạy trận hỗ trợ ước lượng theo cặp ván đổi màu (pentanomial) thì nên dùng, vì nó chặt hơn công thức xấp xỉ ở trên.

SPRT dành cho thay đổi nhằm tăng sức mạnh. Sửa lỗi và refactor không cần chứng minh mạnh hơn, chỉ cần không làm yếu: dùng biến thể non-inferiority (`elo0 = −5, elo1 = 0`), hoặc kiểm bằng perft và `SelfCheck.kt` khi lỗi nằm ở luật.

---

## 3. Quy trình đo

### 3.1. Điều kiện đo cố định

Ghi lại trong báo cáo, cho mọi lần đo:

* Phần cứng và số luồng (mặc định: 1 luồng), kích thước bảng băm của cả hai bên.
* Giới hạn mỗi nước của **cả hai bên** (thời gian, số nút, độ sâu hoặc lượt mô phỏng). Không đo bất đối xứng; nếu bắt buộc, ghi rõ. Ngoại lệ có sẵn trong thiết kế: các cặp có Siêu việt (một bên giới hạn theo thời gian, một bên theo nút) luôn bất đối xứng; nêu rõ trong báo cáo, kèm thời gian đã chuẩn hóa theo `k`.
* Thế mở đầu: danh sách cố định, mỗi thế chơi hai màu, và **số thế khác nhau ≥ số cặp ván** (≥ 100 thế cho 200 ván). Bot giới hạn theo nút hoặc độ sâu, không nhiễu, 1 luồng thì chơi lại y hệt từ cùng một thế; mở đầu lặp cho ra ván trùng, khiến sai số thật lớn hơn bảng 2.2.
* Seed của nhiễu: cố định theo ván (ván i luôn dùng seed i) để lần đo tái lập được, nhưng **khác nhau giữa các ván**.
* Nguồn thế mở đầu: cờ vua và cờ tướng lấy từ bộ thế mở đầu công khai; game không có book (caro, cờ vây, Quoridor, Wall Go) sinh 2 đến 4 nước ngẫu nhiên có seed rồi lọc bằng đánh giá của Siêu việt để giữ thế cân bằng. Ghi cách sinh và cách lọc trong hồ sơ game (mục 12.2).
* Game ván dài (cờ vây, caro): cho phép xử thắng sớm khi đánh giá của Siêu việt vượt một ngưỡng ổn định trong nhiều nước liên tiếp, hoặc khi bot cấp thấp nhận thua. Ghi ngưỡng vào báo cáo và kiểm lại bằng một mẫu ván chơi đến cùng để chắc ngưỡng không làm lệch điểm.
* Phiên bản mã (commit) và **cấu hình của từng bot** (mục 4.4).
* Bảng băm xóa trước mỗi ván.

### 3.2. Hệ số chậm của điện thoại

Máy chủ thường nhanh hơn điện thoại vài lần (phải đo trên thiết bị thật của bạn). Cách xử lý:

1. Chạy bot trên điện thoại thật, khoảng 20 thế ở nhiều giai đoạn ván, lấy trung vị số nút/s `v_phone`; đo bền ít nhất 60 s để thấy hiện tượng giảm xung nhiệt (đo ngắn 5 s sẽ cho `v_phone` quá cao, `k` quá nhỏ, và Siêu việt trên máy đo mạnh hơn trên máy thật). Làm tương tự trên máy đo `v_pc`.
2. Hệ số `k = v_pc / v_phone`.
3. Khi đo trên máy chủ, đặt thời gian bot Siêu việt = `botMillis ÷ k`.

Hai quy tắc đi kèm:

* Các bot **Dễ đến Thách đấu** nên giới hạn theo **độ sâu, số nút hoặc số lượt mô phỏng**, không theo thời gian (mục 4.2). Nhờ vậy sức mạnh của sáu bot này không đổi giữa các máy.
* Bot **Siêu việt** dùng thời gian (`botMillis`), nên sức của nó đổi theo máy: điện thoại chậm thì yếu đi, điện thoại nhanh thì mạnh lên. Đo ở thời gian đã chuẩn hóa theo `k` của **máy tham chiếu** (chiếc điện thoại dùng để đo `v_phone` ở bước 1).

Hệ quả: chỉ cặp Siêu việt vs Thách đấu đổi theo máy, năm cặp kề còn lại thì không. Khoảng đạt của cặp này chỉ được bảo đảm trên máy tham chiếu. Trên máy nhanh hơn, Siêu việt bỏ xa Thách đấu và có thể vượt trần 93%; điều này được chấp nhận vì Siêu việt là toàn lực (mục 1.1). Trên máy chậm hơn, khoảng cách co lại; giới hạn ở đoạn dưới.

Mỗi lần nâng cấp chip hoặc đổi thuật toán nặng hơn, đo lại `k`.

**Máy thấp nhất được hỗ trợ.** Từ Thách đấu trở xuống giới hạn theo nút nên không đổi giữa các máy, còn Siêu việt giới hạn theo thời gian nên số nút nó đạt được đổi theo máy. Trên máy chậm, khoảng cách tài nguyên giữa Siêu việt và Thách đấu có thể co lại nhiều so với tỉ lệ đã chỉnh ở mục 4.3, thậm chí đảo cấp. Đo `k` và nút/giây cả trên máy mạnh lẫn máy thấp nhất định hỗ trợ. Ở máy thấp nhất, số nút Siêu việt đạt được phải gấp ít nhất `2^(200 / g)` lần nút của Thách đấu (≈ 10 lần với `g` = 60, ≈ 4 lần với `g` = 100): đó là tỉ lệ ứng với biên dưới 200 Elo (76%) của cặp Siêu việt vs Thách đấu. Gấp đôi số nút chỉ cho khoảng `g` Elo (60 đến 100), không đủ. Không đạt thì nâng cấu hình máy tối thiểu hoặc giảm giới hạn nút của các cấp dưới (cả thang dịch xuống theo).

Giới hạn nút của mọi cấp dưới phải thỏa `node_limit ≤ 0,5 × (botMillis / 1000) × nps_min` (`botMillis` tính bằng mili giây; `nps_min`: nút mỗi giây đo bền trên máy thấp nhất), để bot nghĩ xong trong khoảng nửa `botMillis` ngay cả trên máy chậm nhất.

**Không** thêm trần thời gian làm giới hạn sức mạnh cho bot giới hạn nút, vì như vậy sức mạnh của nó lại phụ thuộc máy. Ứng dụng vẫn có một **trần an toàn chống treo** bằng `botMillis` (cũng là mốc `p99` ở mục 11.1): nếu chạm trần này, bot trả nước tốt nhất đã có. Trần an toàn không phải công cụ giảm sức, nên chạm nó là lỗi test: đếm tỉ lệ chạm trần trên máy thấp nhất, phải dưới 1%.

### 3.3. Công cụ

Mọi trận đo chạy trên **GitHub Actions** bằng workflow `bot-bench.yml`; mã đo dùng chính engine của app (`core/src/test/.../bench/`). Hướng dẫn đầy đủ: [`tools/bot-bench/README.md`](../tools/bot-bench/README.md).

| Việc | Cách làm |
|---|---|
| Đo hai cấp (mục 3.4) | dòng `ladder <game> <A> <B> pairs=... ms=...` trong `tools/bot-bench/request.txt` |
| Thử cấu hình cấp mà không sửa mã | thêm `props=pm.level.<game>.<cấp>=n15000,p0.05,x20` (xem `BotLevels.override`) |
| So hai biến thể engine (mục 2.6) | dòng `ab <game> <A> <B> games=... ms=...`; người chơi `new`, `old`, `ref`, `smpN`, kèm `@tham số` hoặc `@w:<tag>` |
| Đối chiếu engine ngoài (mục 3.5) | dòng `gauge chess elo=...` (Stockfish biên dịch trên CI) hoặc `gauge xiangqi nodes=...` (Pikafish) |
| Tinh chỉnh Texel (cờ vua, cờ tướng) | `tools/tune/request.txt`, workflow `tune.yml` |

Push `request.txt` lên `main` là chạy: việc được chia ra nhiều máy 4 vCPU (mỗi ván một nhân), kết quả tự commit vào `tools/bot-bench/results/<tag>.txt` và báo cáo `docs/bot-results/`. Không chạy engine đo GPL trên máy phát triển.

### 3.4. Đo 7 bot của một game

Làm theo thứ tự, ghi kết quả vào báo cáo (mục 11.2):

1. **Sáu cặp kề nhau:** Trung bình vs Dễ, Khá vs Trung bình, Giỏi vs Khá, Cao thủ vs Giỏi, Thách đấu vs Cao thủ, Siêu việt vs Thách đấu. Mỗi cặp **≥ 200 ván** (cặp ván đổi màu), riêng Trung bình vs Dễ **≥ 400 ván**. Đạt khi tỉ lệ điểm của bot cấp cao nằm trong khoảng đạt ở mục 1.3.
2. **Năm cặp cách hai cấp** (kiểm tra khoảng cách tối thiểu): Khá vs Dễ, Giỏi vs Trung bình, Cao thủ vs Khá, Thách đấu vs Giỏi, Siêu việt vs Cao thủ. Mỗi cặp **≥ 100 ván**, điểm của bot cấp cao phải đạt ngưỡng tối thiểu ở bảng mục 1.3 (≥ 97%, ≥ 94%, ≥ 88%, ≥ 70%, ≥ 80%). Nếu Elo cộng dồn được thì ngưỡng này đạt khi 6 cặp kề đạt; bước này bắt bất bắc cầu, đảo cấp và lỗi cấu hình (mục 1.3). Quyết định bằng `min_gap_verdict` (mục 2.3): điểm dưới ngưỡng nhưng cận trên khoảng tin cậy còn chạm ngưỡng thì chạy thêm 100 ván rồi quyết theo điểm gộp. Bước chạy thêm này cần thiết vì ngưỡng nằm gần trường hợp xấu nhất; ví dụ Khá vs Dễ ở mức xấu nhất 98,3% vẫn có khoảng 15% khả năng ra dưới 97% sau 100 ván chỉ vì may rủi.
   * **Ba cặp smoke 20 ván** bắt lỗi tráo dòng cấu hình: Siêu việt vs Dễ, Siêu việt vs Khá, Thách đấu vs Dễ. Bot cấp cao phải đạt điểm ≥ 90%.
3. **Quyết định cặp kề theo điểm đo, tối đa hai lần xem** (`pair_verdict`, mục 2.3):
   * Điểm `S` nằm trong khoảng đạt → **đạt**.
   * Khoảng tin cậy 95% không giao khoảng đạt → **không đạt** (quay lại chỉnh, mục 4.3).
   * Còn lại (điểm ngoài khoảng nhưng khoảng tin cậy còn chạm) → chạy thêm **đúng một đợt** bằng số ván ban đầu, gộp, rồi quyết theo điểm gộp: trong khoảng → đạt, ngoài → không đạt. Không chạy đợt thứ ba: xem kết quả nhiều lần rồi dừng khi "vừa lọt khoảng" làm tăng tỉ lệ đạt nhầm, và bot nằm sát biên có thể chạy mãi không ra kết luận.
   * Ván chạy thêm dùng thế mở đầu và seed **chưa dùng** (ví dụ ván 201 đến 400 dùng thế 101 đến 200 và seed 201 đến 400); chạy lại cùng thế và seed chỉ cho ra ván trùng. Vì có tối đa một đợt thêm, bộ thế mở đầu cần đủ cho gấp đôi số ván ban đầu.
4. **Bảng Elo tổng hợp:** đưa kết quả 11 cặp ở bước 1 và 2 vào Ordo hoặc BayesElo (mục 2.4) để báo cáo; chỉ chạy thêm các cặp còn lại khi cần bảng chắc hơn.
5. **Chất lượng nước đi từng cấp:** đo tự động mức hao hụt điểm trung bình (ACPL) hoặc tỉ lệ nước sai nặng của mỗi cấp trên một bộ thế cố định; chỉ số này phải **đơn điệu theo cấp**. Chạy bước này trước khi tốn ván đấu để bắt lỗi thang sớm. Sau đó vẫn xem tay vài ván mỗi cấp để chắc bot không đi nước vô lý (nhất là Dễ và Trung bình).

### 3.5. Đối chiếu với engine ngoài

Muốn biết bot "ngang rung nào" của engine công khai, cho Siêu việt và vài cấp đấu engine ngoài ở thời gian đã chuẩn hóa (cờ vua: Stockfish `UCI_Elo`; cờ tướng: Pikafish theo số nút vì Pikafish không có `UCI_Elo`; cờ vây: KataGo theo số lượt mô phỏng; caro: engine Gomocup). Kết quả chỉ để **báo cáo** (ví dụ "Siêu việt ngang Stockfish `UCI_Elo` X"), **không** là điều kiện đạt. Mỗi mục game có gợi ý đối thủ ngoài. Đây là **engine đo** (mục 4.0): không phát hành nên không bị cổng giấy phép chặn. Game không có engine đo thì bỏ bước này; nghiệm thu chỉ dựa vào mục 3.4.

---

## 4. Cách tạo 7 bot cho một game

### 4.0. Nguồn bot: dùng engine có sẵn hay tự làm

**Quy tắc của dự án.** Với mỗi game, **tra cứu engine có giấy phép cho phép dùng thương mại trước khi viết bot**. Engine nào qua đủ hai cổng dưới đây thì dùng làm **engine nền** (chính là bot Siêu việt) và không tự viết lại. Chỉ tự làm bot cho game không có engine như vậy. Bảy cấp, khoảng đạt và nghiệm thu (mục 1.3 và 11) áp dụng **y nguyên** cho cả hai trường hợp.

Engine ngoài có hai vai trò khác nhau:

* **Engine nền** đi cùng ứng dụng khi phát hành, nên phải qua cả hai cổng.
* **Engine đo** chỉ chạy trên máy test để đối chiếu (mục 3.5), không đóng gói vào ứng dụng, nên không bị cổng giấy phép chặn, kể cả engine GPL. Ghi tên, phiên bản hoặc commit và mã băm của mạng.

**Cổng giấy phép** (đủ cả sáu):

1. Giấy phép của **mã** cho phép dùng thương mại và phân phối trong ứng dụng đóng: MIT, BSD, Apache-2.0, ISC, CC0, Unlicense; MPL-2.0 chỉ khi tuân thủ điều kiện về từng tệp.
2. Giấy phép của **dữ liệu đi kèm** (mạng nơ-ron, trọng số, sách khai cuộc, tablebase) cũng cho phép như vậy. Đây là chỗ hay sót: mã MIT nhưng mạng có giấy phép riêng.
3. GPL, AGPL, LGPL, "non-commercial", "research only" và "không ghi giấy phép" đều **không đạt** để phát hành ứng dụng đóng. Với GPL, trang của các engine như Pikafish và Rapfi nói rõ: khi phân phối phải kèm giấy phép và toàn bộ mã nguồn của đúng bản nhị phân. Muốn dùng engine GPL thì phải chấp nhận nghĩa vụ đó cho ứng dụng; đó là quyết định của bạn, nên hỏi luật sư, tài liệu này không quyết thay.
4. **Giấy phép đúng phiên bản.** Ghim phiên bản hoặc commit và đọc tệp LICENSE ở đúng bản đó; có engine chỉ chuyển sang MIT từ một phiên bản nào đó (Leorik: từ 2.5).
5. Lưu giấy phép và thông báo bản quyền (NOTICE) trong ứng dụng và trong hồ sơ game.
6. Ghi ngày tra cứu và đường dẫn vào hồ sơ game (mục 12.2).

Tài liệu này không phải tư vấn pháp lý; xác nhận cuối cùng với luật sư trước khi phát hành.

**Cổng kỹ thuật** (đủ cả năm):

1. Chạy được trên Android (arm64-v8a; armeabi-v7a nếu còn hỗ trợ) qua NDK/JNI hoặc tiến trình con nói giao thức UCI, GTP...; build và chạy được trên máy thấp nhất, đo RAM và nhiệt (mục 3.2).
2. Chơi **đúng luật của dự án** (hòa, lặp thế, tính điểm, biến thể). Engine chạy sai luật không dùng được dù mạnh; kiểm bằng thế viết tay trước.
3. Điều khiển được tài nguyên: giới hạn theo nút hoặc lượt, độ sâu, thời gian; lý tưởng có MultiPV để chọn nước có nhiễu.
4. Ổn định: 1000 ván tự đấu không treo, không rò bộ nhớ (mục 11.1); chạy được 1 luồng.
5. Kích thước gói, thời gian khởi động và việc tải mạng nằm trong giới hạn của ứng dụng.

**Từ engine nền ra 7 cấp.** Engine nền cấu hình tốt nhất (mạng, bảng băm, luồng, `botMillis`) là Siêu việt. Sáu cấp còn lại là **lớp bọc do ta viết** quanh cùng engine đó: giới hạn nút hoặc độ sâu theo công thức mục 4.3, chọn nước có nhiễu trong nhóm điểm (MultiPV) và lớp an toàn (mục 4.2). Tùy chọn tự giảm sức của engine (như `UCI_Elo`, `Skill Level`) chỉ là một công cụ trong lớp bọc, **không tin nhãn Elo của chúng**: mọi cấp vẫn phải qua khoảng đạt ở mục 1.3. Khi engine nền đổi phiên bản hoặc mạng thì Siêu việt đổi, phải đo lại (mục 11.1).

**Game không có engine nền:** tự làm theo mục 5 đến 10 và phụ lục A. Có engine đo thì dùng để đối chiếu (mục 3.5). Không có engine đo thì tự đánh giá bằng 7 bot đấu nhau theo mục 3.4, kèm bộ thế chiến thuật của game.

**Kết quả tra cứu ngày 27/11/2026** (theo trang dự án và tài liệu công khai; phải xác minh lại đúng phiên bản trước khi dùng):

| Game | Engine tìm thấy | Giấy phép theo nguồn tìm được | Kết luận |
|---|---|---|---|
| Cờ vua | Stockfish | GPL-3.0 | Chỉ làm **engine đo** |
| Cờ vua | Leorik (từ 2.5), Zahak, Arasan, Caissa | MIT (Leorik: README; Zahak: chessprogramming wiki; Arasan, Caissa: theo diễn đàn TalkChess) | **Ứng viên engine nền**; kiểm LICENSE đúng phiên bản, ngôn ngữ, khả năng build NDK, và giấy phép mạng nếu có |
| Cờ tướng | Pikafish | GPL-3.0; mạng có điều khoản riêng (trang chính ghi dữ liệu huấn luyện theo ODbL, một dự án dẫn xuất ghi giấy phép mạng cấm dùng thương mại khi chưa xin phép; phải đọc trực tiếp) | **Engine đo**; chưa thấy engine nền đạt cổng, mặc định **tự làm** |
| Cờ caro | Rapfi | GPL-3.0 | **Engine đo** (chỉ hợp lệ nếu chạy đúng luật, mục 7.5) |
| Cờ caro | figrid (MIT hoặc Apache-2.0), Chis (MPL), PentaZen, SlowRenju | Khác nhau; các engine Gomocup chơi luật freestyle, standard hoặc renju, khác luật chặn hai đầu của dự án | Chưa đạt cổng luật, mặc định **tự làm** |
| Cờ vây | KataGo | Mã MIT; mạng g170 b6c96 và b10c128: CC0; mạng khác và mô hình HumanSL: "KataGo Neural Network License" (đọc trước khi dùng) | **Engine nền** (mục 8), nếu qua cổng kỹ thuật |
| Quoridor | Dự án sở thích và bài tập | Không đồng nhất | **Tự làm**; có thể dùng làm đối thủ tham khảo |
| Wall Go | Bản remake (MIT, AI yếu), WallZero (mã nghiên cứu) | MIT / chưa rõ | **Tự làm** |

### 4.1. Nguyên tắc: làm Siêu việt trước, giảm sức dần xuống

1. **Bot Siêu việt trước.** Làm cho bot toàn lực mạnh nhất có thể (cải tiến từng bước qua SPRT, mục 2.6). Mọi cải tiến thuật toán của game nhằm vào bot này. Với game dùng engine nền (mục 4.0), "làm Siêu việt" nghĩa là chọn phiên bản, mạng và tham số tốt nhất của engine nền, mỗi thay đổi vẫn qua SPRT.
2. **Chốt Siêu việt rồi mới làm sáu cấp còn lại.** Mỗi lần Siêu việt mạnh thêm đáng kể, khoảng cách với Thách đấu rộng ra và phải chỉnh lại từ trên xuống. Vì vậy chỉ chỉnh các cấp dưới khi đã có bản Siêu việt dự kiến phát hành.
3. **Sáu cấp còn lại là bản giảm sức của cùng một bot.** Dùng cùng mã luật, cùng hàm đánh giá, chỉ khác giới hạn và nhiễu (mục 4.2). Không viết bot riêng cho từng cấp trừ khi cần, vì khó duy trì. Với engine nền, "cùng một bot" là engine nền cộng lớp bọc (mục 4.0); giới hạn, nhiễu và lớp an toàn đều nằm ở lớp bọc.
4. **Chỉnh từ trên xuống, mỗi cấp so với cấp kề trên** (mục 4.3).

### 4.2. Hai công cụ giảm sức

* **Giới hạn tài nguyên:** độ sâu tối đa, số nút, số lượt mô phỏng hoặc thời gian mỗi nước. Đây là cách ổn định nhất. Cấp Dễ đến Thách đấu nên giới hạn theo **độ sâu hoặc số nút** để sức mạnh không phụ thuộc điện thoại (mục 3.2).
* **Nhiễu có kiểm soát:** với xác suất `p`, chọn một nước trong nhóm nước tốt (ví dụ nước tốt thứ hai hoặc thứ ba, chỉ trong những nước có điểm cách nước tốt nhất không quá `δ`) thay vì nước tốt nhất. Cấp càng thấp thì `p` càng lớn và `δ` càng rộng.

Ba quy tắc cho mọi cấp:

* **Không bao giờ bỏ nước thắng ngay hoặc nước cứu thua trong một nước.** Người chơi chấp nhận bot dở nhưng không chấp nhận bot tự đưa mình vào thế thua ngớ ngẩn.
* **Không làm yếu bằng nước vô lý.** Người chơi nhận ra ngay và mất niềm tin. Bot yếu phải "nhìn nông", không phải "đi bừa".
* **Mọi cấu hình phải tái lập được.** Cùng cấu hình và cùng seed thì cho cùng kết quả khi kiểm thử.

Cách bảo đảm quy tắc đầu: đặt một **lớp an toàn chạy trước nhiễu, độc lập với giới hạn độ sâu**. Lớp này (a) đi ngay nước thắng nếu có; (b) loại khỏi nhóm nước được chọn mọi nước khiến đối phương có đòn thắng ngay sau đó; (c) chỉ khi cả hai không xảy ra mới cho nhiễu chọn. Không thể trông vào tìm kiếm để làm việc này ở cấp Dễ, vì độ sâu 1 không nhìn thấy nước đáp trả của đối phương. Nếu mọi nước đều khiến đối phương thắng ngay (thế đã thua), bỏ bước (b) và chọn theo tìm kiếm bình thường. Mỗi hồ sơ game khai báo hai vị từ `win_in_1` và `lose_in_1` (mục 12.2), hoặc ghi "không áp dụng" kèm lý do (cờ vây: xem mục 8.2). Lớp an toàn làm cấp Dễ mạnh hơn độ sâu 1 thuần, nên chỉnh Dễ bằng đo; nếu Trung bình vs Dễ vẫn không đạt thì hạ nút hoặc độ sâu của Dễ và tăng nhiễu trong giới hạn "không vô lý" (chỉ cấm bỏ đòn thắng hoặc thua trong một nước, không cấm đi mất quân). Game chưa phát hành cho đến khi đạt (mục 11.3).

### 4.3. Vòng chỉnh từng cấp

Làm lần lượt: Thách đấu (so với Siêu việt), Cao thủ (so với Thách đấu), Giỏi, Khá, Trung bình, Dễ.

1. Đặt cấu hình khởi điểm của cấp (bảng trong mục game). Với các cấp giới hạn theo nút hoặc lượt, tính tỉ lệ khởi điểm từ khoảng Elo ở mục 1.2: `tỉ lệ = 2^(−khoảng Elo / g)`, trong đó `g` là số Elo thu được mỗi lần nhân đôi số nút (hoặc lượt) của game đó.
   * Đo `g` một lần cho mỗi game: cho bot N nút đấu bot 2N nút, **ít nhất 400 ván, nên 1000** (sai số ±35 và ±22 Elo; 100 ván cho ±70, lớn hơn chính `g`); `g` ≈ Elo tính từ tỉ lệ điểm của bot 2N (mục 2.1). Giá trị tham khảo, phải đo lại: khoảng 50 đến 70 với alpha-beta (cờ vua, cờ tướng), khoảng 100 với MCTS.
   * Ví dụ `g = 60`: cách 200 Elo ứng với khoảng 1/10 số nút, cách 300 Elo ứng với khoảng 1/32. Với `g = 100`: 1/4 và 1/8.
   * `g` không phải hằng số: với alpha-beta nó giảm khi số nút lớn. Đo ở 2 đến 3 vùng (gần Khá, gần Giỏi và Cao thủ, gần Thách đấu và Siêu việt) và dùng `g` cục bộ cho từng cặp.
   * Tài nguyên phải tăng ít nhất ×2 giữa hai cấp kề. Quy đổi cấp dùng độ sâu về số nút trung bình đo được để kiểm quy tắc này giữa Khá (độ sâu) và Giỏi (nút). Nếu tính ra không đạt thì Siêu việt quá yếu so với thang (xem mục 8, ý thứ ba).
2. Cho cấp cần chỉnh đấu cấp kề trên **100 ván** (đổi màu). Tính tỉ lệ điểm của cấp trên.
3. So với khoảng đạt ở mục 1.3:
   * **Dưới biên thấp:** cấp dưới quá mạnh. Giảm tài nguyên (ví dụ nút ×0,6, hoặc bớt 1 độ sâu) hoặc tăng nhiễu.
   * **Trên biên cao:** cấp dưới quá yếu. Tăng tài nguyên (nút ×1,6, hoặc thêm 1 độ sâu) hoặc giảm nhiễu.
   * **Trong khoảng:** chuyển sang bước 4.
4. **Xác nhận 200 ván** (400 ván cho cặp Trung bình vs Dễ), kết luận theo quy tắc mục 3.4 bước 3, và ghi cấu hình cuối cùng vào hồ sơ game (mục 12.2). Sau đó **không đổi** nữa, trừ khi Siêu việt đổi.
5. Làm cấp kế tiếp phía dưới.

Mẹo để tiết kiệm thời gian:

* Các cấp thấp chơi rất nhanh (độ sâu nhỏ), nên đo rẻ; phần tốn thời gian là các cặp ở trên cùng (Siêu việt dùng cả `botMillis`).
* Dùng một đối thủ cố định cho vòng chỉnh sơ bộ (10 đến 30 ván) để thu hẹp khoảng tìm, rồi mới chạy 100 ván.
* Chỉnh **một tham số mỗi lần** (hoặc tài nguyên, hoặc nhiễu), để biết thay đổi nào gây ra hiệu quả.
* Với các cặp có Siêu việt (tốn nhất), chỉnh ở ngân sách rút gọn xấp xỉ đối xứng rồi xác nhận lần cuối ở ngân sách thật.

### 4.4. Mẫu cấu hình mỗi cấp

Mỗi cấp ghi một dòng, lưu trong hồ sơ game và trong mã (một bảng cấu hình duy nhất, không rải rác):

```
Cấp: ...              Giới hạn: độ sâu / nút / lượt mô phỏng = ...
Nhiễu: p = ...        δ (điểm chênh tối đa với nước tốt nhất) = ...
Thời gian tối đa mỗi nước: ...    Nhịp giả lập (humanDelay): ...
Tỉ lệ điểm đo được so với cấp kề trên: ... (200 ván)
```

### 4.5. Nhịp đi và hiển thị

* Cấp thấp không nên đi tức thì: thêm độ trễ ngắn tự nhiên (`BotNames.humanDelay()`), để trải nghiệm giống đấu với người.
* Ngược lại, độ trễ thêm này **không** được cộng vào thời gian suy nghĩ của bot khi đo.
* Hiển thị **tên cấp** (Dễ đến Siêu việt) kèm mô tả ngắn ở cột "Hành vi mong đợi" (mục 1.1). Không hiển thị số Elo trong app, và không viết "ngang người chơi cấp ...".
* Chế độ mặc định nên là **Trung bình** hoặc **Dễ** cho người mới (tùy chọn sản phẩm). Người chơi luôn đổi được cấp.

---

## 5. Cờ vua

**Nguồn bot (mục 4.0):** Stockfish là GPL-3.0, chỉ dùng làm engine đo. Ứng viên engine nền giấy phép MIT: Leorik (từ 2.5), Zahak, Arasan, Caissa; phải xác minh đúng phiên bản. Nếu chọn engine nền thì mục 5.2 và phụ lục A không áp dụng; mục 5.1 (cổng) vẫn áp dụng cho luật và tích hợp của ta, mục 5.3 áp dụng cho lớp bọc.

### 5.1. Cổng bắt buộc

Các cổng này áp dụng cho bot Siêu việt (cùng mã luật và đánh giá với các cấp khác):

| Cổng | Cách kiểm | Mốc |
|---|---|---|
| Luật (perft) | Thế ban đầu (initial position): độ sâu 4 = 197.281, độ sâu 5 = 4.865.609 · "Kiwipete" (`r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq -`): độ sâu 3 = 97.862, độ sâu 4 = 4.085.603 | Khớp tuyệt đối |
| Luật đặc biệt | Nhập thành, bắt tốt qua đường, phong cấp, hòa luật 50 nước, lặp thế 3 lần, thiếu quân | Mọi thế trong `SelfCheck.kt` qua |
| Chiến thuật | Bộ thế như *Win At Chess* (WAC), *Bratko-Kopec* | Chốt số thế giải được ở 1 s làm mốc từ lần đo đầu tiên, rồi không để hồi quy |
| Chất lượng nước đi | Tổng hao hụt điểm trung bình (ACPL) so với Stockfish độ sâu cao trên 50 ván tự đấu | Chốt mốc từ lần đo đầu, không để tăng quá 10% |
| Ổn định | 1000 ván tự đấu (trộn các cấp) | 0 treo, 0 quá giờ, 0 nước sai luật |

### 5.2. Làm bot Siêu việt mạnh hơn

Chỉ làm **một thay đổi mỗi lần** và đo SPRT:

1. **Tinh chỉnh tham số đánh giá bằng dữ liệu** (Texel tuning): thường mang lại nhiều nhất cho công sức bỏ ra.
2. **Quản lý thời gian:** dừng sớm khi nước đi ổn định, kéo dài khi điểm tụt; kiểm tra giờ đủ dày (phụ lục A).
3. **Thứ tự nước đi:** countermove, continuation history; đo kỹ vì dễ gây lỗi hồi quy.
4. **Cắt tỉa và giảm độ sâu:** tinh chỉnh LMR, null-move, futility theo đo A/B.
5. **Đa luồng Lazy SMP** nếu điện thoại có nhiều nhân; cân nhắc pin và nhiệt.
6. **Tàn cuộc:** bảng tàn cuộc nhỏ (như K+P đối K) và quy tắc hòa lý thuyết.
7. **Mạng nơ-ron nhỏ (NNUE):** giữ nguyên phần tìm kiếm, chỉ thay `evaluate()` bằng mạng huấn luyện từ dữ liệu tự đấu. Chỉ cần khi mục 1 đến 6 đã hết dư địa.

### 5.3. Cấu hình 7 bot: mẫu khởi điểm (cấu hình đã đo và chốt: mục 13.1)

Các giá trị dưới đây là điểm bắt đầu khi làm một engine mới, **không phải kết quả đo**. Cấu hình đang dùng trong `BotLevels.kt` và tỉ lệ đo được nằm ở mục 13.

| Cấp | Giới hạn tìm kiếm | Nhiễu |
|---|---|---|
| Dễ | Độ sâu tối đa 1 đến 2 | Cao |
| Trung bình | Độ sâu tối đa 3 đến 4 | Vừa |
| Khá | Độ sâu tối đa 5 đến 6 | Thấp |
| Giỏi | Giới hạn nút, ≈ 1/10 nút của Cao thủ | Rất thấp hoặc không |
| Cao thủ | Giới hạn nút, ≈ 1/10 nút của Thách đấu | Không |
| Thách đấu | Giới hạn nút, ≈ 1/32 nút của Siêu việt | Không |
| Siêu việt | Toàn lực, thời gian `botMillis` | Không |

Các tỉ lệ nút tính với `g = 60` (mục 4.3); đo lại `g` rồi tính lại.

### 5.4. Đối chiếu engine ngoài (tùy chọn)

Stockfish có `UCI_LimitStrength=true` và `UCI_Elo` (mức thấp nhất khoảng 1320 tùy phiên bản; thang này được neo theo CCRL, không phải FIDE). Để biết Siêu việt và các cấp "ngang rung nào", cho đấu Stockfish ở vài mức `UCI_Elo`, cùng thời gian đã chuẩn hóa, 1 luồng, ≥ 100 ván mỗi mức, chọn mức sao cho tỉ lệ điểm nằm trong 20% đến 80%. Chỉ để báo cáo, không phải điều kiện đạt. Dễ và Trung bình thường nằm dưới mức thấp nhất của `UCI_Elo` nên không đo được bằng cách này.

### 5.5. Trạng thái 11/2026

* **Engine Siêu việt** (`ChessEngine.kt`, tự làm, alpha-beta): bitboard với magic, PVS lặp sâu dần có cửa sổ hẹp, Lazy SMP, bảng băm dùng chung, null move, reverse futility, razoring, futility, late move pruning, cắt tỉa theo SEE và history, LMR, singular extension, internal iterative reduction, killer, countermove, history, continuation history, capture history, correction history theo cấu trúc tốt. Đánh giá tuyến tính 545 tham số (tapered), tinh chỉnh Texel trên khoảng 1,5 triệu thế tự đấu sinh trên CI (workflow `tune.yml`, đợt `chess-t2`, rồi `chess-t3` tinh chỉnh tiếp). Tham số tìm kiếm nằm ở `object SP`, mỗi giá trị đã qua A/B trên CI.
* **Các bước đã đo trên CI** (A/B 200 ms/nước, 1 luồng): trọng số Texel đầu tiên +73 Elo; correction history +40; tổ hợp tham số tìm kiếm `qd240 futd130 asp12 hp2500` +26; trọng số `chess-t2` +112 (1008 ván); trọng số `chess-t3` +64 (1008 ván); trọng số `chess-t4` +24 (1008 ván, `g14`, đang dùng; nghiệm thu lại ở `g16`: đạt). Lazy SMP 3 luồng hơn 1 luồng +119 Elo ở 1 s/nước.
* **Đối chiếu Stockfish 17.1** (biên dịch trên CI, `UCI_LimitStrength`, 1 luồng; ta 3 luồng; cả hai 2 s/nước). Đợt `g16`, trọng số `chess-t4` (đang dùng): `UCI_Elo` 2800 → **58,3%** (120 ván, +39 =62 −19, ước tính 2858); `UCI_Elo` 3000 → 43,3% (60 ván, ước tính 2953). Đợt `g13`, trọng số `chess-t3`: `UCI_Elo` 2800 → **62,5%** (120 ván, +43 =64 −13, ước tính 2889); `UCI_Elo` 3000 → 36,7% (60 ván, ước tính 2905). Đợt `g9` (trọng số `chess-t2`): 2800 → 54,4% (80 ván), 3000 → 40% (40 ván). **Điều kiện đạt (≥ 50% trước `UCI_Elo` 2800, ≥ 40 ván, 2 s/nước) đã thỏa.** **`UCI_Elo` của Stockfish được neo theo thang CCRL (danh sách xếp hạng engine), không phải FIDE**: "ngang `UCI_Elo` 2800" nghĩa là ngang Stockfish ở mức giới hạn đó trên thang CCRL, không phải ngang kỳ thủ FIDE 2800. Số đo ở thời gian của máy CI (4 vCPU); trên điện thoại, Siêu việt mạnh hay yếu hơn theo hệ số `k` (mục 3.2).

---

## 6. Cờ tướng

**Nguồn bot (mục 4.0):** Pikafish là GPL-3.0 và mạng có điều khoản riêng, nên mặc định chỉ dùng làm engine đo. Chưa thấy engine nền đủ giấy phép, nên mặc định tự làm. Dùng Pikafish làm engine nền chỉ khi bạn chấp nhận nghĩa vụ GPL cho ứng dụng hoặc xin phép tác giả (mục 4.0, cổng giấy phép, ý 3).

### 6.1. Cổng bắt buộc

| Cổng | Cách kiểm | Mốc |
|---|---|---|
| Luật (perft) | Thế ban đầu (initial position): độ sâu 1 = 44, 2 = 1.920, 3 = 79.666, 4 = 3.290.240, 5 = 133.312.995 (đối chiếu lại với một engine tham chiếu như Pikafish trước khi tin) | Khớp tuyệt đối |
| Luật đặc thù | Hai tướng không được đối mặt (tướng "lộ mặt"), pháo ăn qua một ngòi, mã bị cản, tượng bị chẹn mắt, sĩ và tướng chỉ trong cung | Mọi thế kiểm thử qua |
| Luật lặp thế | Chiếu dài, đuổi dài, lặp thế: chọn đúng một bộ luật (nêu rõ trong app) và kiểm thử từng tình huống | Kết quả đúng với bộ luật đã chọn |
| Chiến thuật | Thế chiếu bí, thế "pháo ghim tướng", tàn cuộc xe, pháo, mã | Chốt mốc từ lần đo đầu, không hồi quy |
| Ổn định | 1000 ván tự đấu (trộn các cấp) | 0 treo, 0 quá giờ |

### 6.2. Làm bot Siêu việt mạnh hơn

* **Hàm đánh giá** là phần cần đầu tư nhất: giá trị pháo và mã đổi theo giai đoạn; sĩ + tượng có giá trị phòng thủ tăng theo lực tấn công đối phương. Tinh chỉnh bằng dữ liệu tự đấu hoặc dùng mạng nơ-ron.
* **Điểm hòa nên hơi âm** cho bot (contempt) để tránh chấp nhận hòa khi đang ngang hoặc hơn. Chỉ áp dụng cho Giỏi trở lên; cấp thấp không cần.
* Ván hòa tính 0,5. Hòa nhiều làm phương sai mỗi ván nhỏ đi (mục 2.2), nên không cần thêm ván so với cờ vua; giữ đúng số ván ở mục 3.4.
* Cùng khung tìm kiếm như cờ vua (phụ lục A.2), tham số riêng ở bảng A.2.

### 6.3. Cấu hình 7 bot: mẫu khởi điểm (cấu hình đã đo và chốt: mục 13.1)

| Cấp | Giới hạn tìm kiếm | Nhiễu |
|---|---|---|
| Dễ | Độ sâu tối đa 1 đến 2 | Cao |
| Trung bình | Độ sâu tối đa 3 đến 4 | Vừa |
| Khá | Độ sâu tối đa 5 đến 6 | Thấp |
| Giỏi | Giới hạn nút, ≈ 1/10 nút của Cao thủ | Rất thấp hoặc không |
| Cao thủ | Giới hạn nút, ≈ 1/10 nút của Thách đấu | Không |
| Thách đấu | Giới hạn nút, ≈ 1/32 nút của Siêu việt | Không |
| Siêu việt | Toàn lực, thời gian `botMillis` | Không |

Các tỉ lệ nút tính với `g = 60` (mục 4.3); đo lại `g` rồi tính lại.

### 6.4. Đối chiếu engine ngoài (tùy chọn)

Pikafish dùng mạng nơ-ron nên giới hạn theo **số nút**, không theo Elo. Dựng một thang vài rung, ví dụ 30, 100, 300, 1.000, 3.000, 10.000, 30.000, 100.000 nút, cho Siêu việt và vài cấp đấu từng rung (cùng thời gian chuẩn hóa, ≥ 100 ván mỗi rung). Chỉ để báo cáo "ngang rung nào", không là điều kiện đạt.

### 6.5. Trạng thái 11/2026

* **Engine Siêu việt** (`XiangqiEngine.kt`, tự làm): danh sách quân và bảng nước đi tính sẵn, cùng khung tìm kiếm với cờ vua (PVS, Lazy SMP, null move, LMR, singular, SEE, history các loại, correction history), đánh giá tuyến tính 711 tham số tinh chỉnh Texel trên khoảng 3,1 triệu thế tự đấu (đợt `xiangqi-t1`). Luật app: hết nước là thua, lặp thế thì bên vừa đi thua.
* **A/B trên CI:** engine mới hơn engine cũ +267 Elo (240 ván, 1 s/nước); trọng số tinh chỉnh hơn trọng số khởi điểm +236 (408 ván). Correction history chỉ +15 (không có ý nghĩa thống kê), vẫn giữ mức 32. Tham số tìm kiếm `object XSP` (g21, 408 ván mỗi biến thể): rfpm80+rfpi60 −9, futb120+futd130 −4, hp2500 −8, asp12+qd240 +14, đều trong sai số nên giữ mặc định. Trọng số Texel đợt 2 `xiangqi-t2` (4,38 triệu thế, sai số 0,1225 → 0,1193): +30 Elo (g22, 408 ván, khoảng tin cậy chạm 50%), đang đo thêm 400 ván (g24).
* **Đối chiếu Pikafish:** bản phát hành 2026 không có `UCI_Elo` / `UCI_LimitStrength`, nên chỉ giới hạn được theo số nút mỗi nước (dòng `gauge xiangqi nodes=...`). Với 20.000 nút/nước, Pikafish vẫn thắng áp đảo (ta 12,5%, 20 ván): Siêu việt còn cách xa engine NNUE hàng đầu. Đây là số đo báo cáo, không phải điều kiện đạt.

---

## 7. Cờ caro

**Nguồn bot (mục 4.0):** Rapfi là GPL-3.0 (engine đo). Các engine Gomocup khác chơi luật freestyle, standard hoặc renju, không có luật "chặn hai đầu" của dự án, nên mặc định tự làm; chỉ bọc engine có sẵn nếu chứng minh được nó chơi đúng luật của dự án.

### 7.1. Cổng bắt buộc

| Cổng | Cách kiểm | Mốc |
|---|---|---|
| Luật thắng | 5 quân trở lên thắng, trừ khi **cả hai đầu bị quân đối phương chặn** (luật hiện tại của dự án); kiểm thử từng tình huống (đúng 5, 6 quân, bị chặn một đầu, bị chặn hai đầu). "Đầu" là hai ô liền kề hai đầu của **chuỗi cực đại**: quân cùng màu nối dài là một phần của chuỗi, không phải chặn. Module VCF/VCT phải dùng đúng định nghĩa này (một "bốn" mà thành năm bị chặn hai đầu không phải đe dọa) | Khớp luật đã chọn |
| Bàn vô hạn | Các bài `caro vo han: ...` trong `SelfCheck.kt`; thêm thế có quân rải rộng quá cửa sổ 24×24 | Qua toàn bộ |
| VCF | Bộ thế "thắng bằng chuỗi bốn" với độ sâu 3 đến 24 nước | Siêu việt giải 100% |
| Phòng thủ VCF | Thế mà đối phương sắp có VCF: bot phải chọn nước phá | Siêu việt phòng đúng 100% |
| VCT (nếu bật) | Thế thắng bằng chuỗi có "ba hở" | Chỉ bật khi SPRT chứng minh có lợi |
| Ổn định | 1000 ván tự đấu (trộn các cấp) | 0 treo, 0 quá giờ |

### 7.2. Làm bot Siêu việt mạnh hơn

* Bật lại VCT sau khi sửa: cho mọi nước đỡ của đối phương đều phải thua, giới hạn thời gian module ≤ 10 đến 20% ngân sách (bài học 2 ở phụ lục A.4); chỉ giữ nếu SPRT cho thấy có lợi.
* Xử lý bàn rộng: thay cửa sổ cố định bằng danh sách ô lân cận quân (không cắt quân xa) nếu đo cho thấy thua vì bỏ sót quân.

### 7.3. Lưu ý riêng của caro

* Lợi thế đi trước rất lớn. Mọi trận đo đều đấu **cặp ván đổi màu**, mở đầu bằng thế cân bằng 2 đến 3 nước ngẫu nhiên hoặc luật hoán đổi (mục 2.5). Báo cáo thêm tỉ lệ điểm riêng khi đi trước và đi sau của từng cặp.
* Caro hầu như không có hòa nên phương sai mỗi ván lớn nhất (bảng 2.2 chính là trường hợp của caro): kết quả **không** hội tụ nhanh hơn cờ vua, và không được bớt số ván.

### 7.4. Cấu hình 7 bot: mẫu khởi điểm (cấu hình đã đo và chốt: mục 13.1)

Ở caro, cấp được tạo bằng cách **bật dần module đe dọa**, vì đây là thứ phân biệt người chơi giỏi và yếu.

| Cấp | Tìm kiếm | Module đe dọa |
|---|---|---|
| Dễ | Độ sâu 1 đến 2, nhiễu cao | Chỉ chặn bốn trực tiếp |
| Trung bình | Độ sâu 2 đến 3, nhiễu vừa | Chặn thêm ba hở |
| Khá | Độ sâu 4 đến 6, nhiễu thấp | VCF ngắn (≤ 5 nước) |
| Giỏi | Giới hạn nút, ≈ 1/10 nút của Cao thủ | VCF sâu (≤ 12 nước) + phòng VCF |
| Cao thủ | Giới hạn nút, ≈ 1/10 nút của Thách đấu | VCF đầy đủ + phòng VCF |
| Thách đấu | Giới hạn nút, ≈ 1/32 nút của Siêu việt | Như Siêu việt |
| Siêu việt | Toàn lực, thời gian `botMillis` | VCF + phòng VCF, thêm VCT nếu qua SPRT |

Các tỉ lệ nút tính với `g = 60` (mục 4.3); đo lại `g` cho caro rồi tính lại.

### 7.5. Đối chiếu engine ngoài (tùy chọn)

Một engine Gomocup (như Rapfi) bị giới hạn nút, độ sâu hoặc thời gian. Cho Siêu việt đấu vài mức giới hạn, ≥ 100 ván mỗi mức. Chỉ để báo cáo. Chỉ hợp lệ nếu engine chạy đúng luật caro của dự án (Gomocup thường dùng freestyle, standard hoặc renju, khác luật chặn hai đầu): kiểm bằng thế viết tay trước, không giả định.

### 7.6. Trạng thái 11/2026

* **Engine Siêu việt** (`CaroEngine.kt`, tự làm). Mỗi ô trống, mỗi hướng có khóa màu 10 ô lân cận (5 ô mỗi phía). Bảng `CaroPat` tính sẵn hình cờ theo định nghĩa đệ quy (năm, bốn hở, bốn, ba hở…), nên tự đúng với luật chặn hai đầu và hình gãy. Cửa sổ 5 ô mỗi phía là bắt buộc: với 4 ô, thế `O X X X X _ O` bị nhận nhầm là bốn. Đánh giá và lớp ô (4-4, 4-3, 3-3) cập nhật tăng dần. Tìm kiếm có VCF, phòng VCF, PVS lặp sâu, bảng băm, killer; đối phương có ba hở thì chỉ xét nước chặn và nước đánh bốn. Bàn vô hạn: cửa sổ 32×32 quanh các quân, ngoài cửa sổ coi là trống.
* **Luật đỡ bốn.** Một hàng bốn có một đầu bị chặn đỡ được bằng **hai** cách: chiếm ô thành năm, hoặc đặt quân ở đầu kia để dãy năm bị chặn hai đầu (`O X X X X _ .` thì đặt vào ô `.`). VCF và nước bắt buộc phải xét cả hai (`defenses()` thử từng ô trên đường), nếu không sẽ báo thắng sai.
* **A/B trên CI:** engine mới hơn engine cũ +232 Elo ở 1 s/nước (408 ván, 79%).
* **7 cấp:** cả 6 cặp kề và 5 cặp cách hai cấp đều đạt (mục 13). Hai cấp dưới cùng dùng **nhiễu rộng**: với xác suất `p`, bot đi một nước ngẫu nhiên gần quân (delta ≥ `RANDOM_DELTA` trong `Gomoku.kt`), trừ khi đang có nước thắng, nước chặn bắt buộc hoặc VCF. Nhiễu theo delta thường bão hòa ở độ sâu 1 vì lớp an toàn luôn chặn bốn, nên không đủ hạ sức cấp Dễ.

---

## 8. Cờ vây

Dự án dùng bàn **19×19**, luật diện tích (Tromp-Taylor hoặc luật Trung Quốc), komi 7,5. Chọn **một** bộ luật và dùng đúng bộ đó cho mọi bot. Hai bộ này khác nhau: Tromp-Taylor gốc (quy tắc 7) cho phép tự sát nhiều quân và đếm quân còn trên bàn khi kết thúc (không có thỏa thuận quân chết); biến thể không tự sát (NSTT) và luật Trung Quốc thì cấm tự sát. KataGo có tùy chọn cho phép hoặc cấm tự sát nhiều quân, nên cấu hình đúng bộ luật đã chọn. Hồ sơ game phải ghi rõ bộ nào và tính hợp lệ của tự sát. Cờ vây chạy trên điện thoại gần như bắt buộc dùng MCTS (tìm kiếm Monte Carlo) vì không có hàm đánh giá viết tay đủ tốt.

Bàn 19×19 khó hơn bàn nhỏ ở ba điểm cần tính từ đầu:

* **Ván rất dài** (vài trăm nước), nên đo 200 ván tốn thời gian hơn nhiều so với cờ vua. Chạy song song nhiều ván trên máy nhiều nhân, đo các cấp thấp bằng giới hạn lượt mô phỏng nhỏ (rất nhanh), và dùng SPRT khi chỉnh Siêu việt.
* **Số lượt mô phỏng mỗi giây trên điện thoại thấp** so với kích thước bàn, và cây tìm kiếm tốn RAM. Hai chỉ số này quyết định sức mạnh của bot Siêu việt hơn là thuật toán.
* **Khoảng cách giữa Siêu việt và các cấp dưới phụ thuộc vào Siêu việt mạnh đến đâu.** Siêu việt càng yếu thì sáu cấp dưới càng bị nén lại; nếu quá nén thì không đủ chỗ để xếp 7 cấp đúng khoảng đạt. Khi đó phải tăng sức Siêu việt chứ không nên ép các cấp dưới.

**Quyết định đã chốt:** giữ bàn 19×19 và không phát hành thang nén (mục 11.3). Hai điều đó buộc cờ vây dùng **mạng chính sách/giá trị**, vì MCTS thuần không đủ mạnh ở 19×19 trên điện thoại. Engine nền dự kiến: **KataGo** (mục 4.0). Vì chưa có gì được đo, phải qua **cổng khả thi** sau trước khi làm tiếp; ghi kết quả vào hồ sơ game:

1. **Chạy được trên máy thấp nhất.** KataGo chính thức hỗ trợ Windows và Linux; cho Android phải tự build bằng NDK (backend Eigen chạy CPU) hoặc chạy mạng đã lượng tử hóa (TFLite) dưới lớp tìm kiếm. Cộng đồng đã có ứng dụng Android dùng KataGo nên khả thi, nhưng phải chứng minh trên máy của bạn: lượt/giây **bền** (≥ 60 s), RAM, nhiệt, kích thước gói.
2. **Đủ dải sức mạnh.** Bảng của KataGo (cùng 1200 lượt, trong bể đối thủ của KataGo, không phải trên điện thoại) cho mạng 6 khối ≈ −1180, 10 khối ≈ −280, 15 khối ≈ +510, 20 khối ≈ +1100 đến +1170. Tức đổi kích thước mạng cho chênh hàng trăm Elo mỗi bậc, nên kích thước mạng cộng số lượt có thể phủ dải 2000 Elo của thang. Phải đo lại trên điện thoại.
3. **Đáy thang.** Mạng nhỏ nhất vẫn rất mạnh ngay ở 1 lượt, nên Dễ phải làm yếu bằng lấy mẫu theo chính sách (nhiệt độ cao), mạng nhỏ nhất, 1 lượt và lớp an toàn (không tự lấp mắt, không tự sát). Mô hình HumanSL của KataGo mô phỏng hạng 20 kyu đến 9 dan, nhưng nặng khoảng 99 MB (bản TFLite lượng tử hóa khoảng 28 MB), có giấy phép riêng, và người dùng diễn đàn báo hạng danh nghĩa ở mức thấp không khớp thực tế; nếu dùng thì **đo, không tin nhãn**.

Nếu (1) hoặc (3) không qua trên máy thấp nhất, cờ vây **chưa phát hành**; đòn bẩy còn lại là nâng cấu hình máy tối thiểu hoặc đổi cấu hình mạng, không phải hạ chuẩn.

### 8.1. Cổng bắt buộc

| Cổng | Cách kiểm | Mốc |
|---|---|---|
| Luật | Bắt quân, tự sát, ko / siêu ko (superko), kết thúc bằng hai lần bỏ lượt, đếm điểm diện tích | Qua bộ thế viết tay |
| Sống chết (tsumego) | Bộ bài sống chết (cục bộ và toàn bàn) | Chốt mốc từ lần đo đầu, không hồi quy |
| Đọc thang (ladder) | Thế thang chạy được và không chạy được | Đúng cả hai |
| Kết thúc ván | Bot biết kết thúc đúng lúc, không lấp mắt của chính mình, xử lý quân chết đúng theo bộ luật đã chọn (Tromp-Taylor: bắt hết quân chết trên bàn trước khi bỏ lượt) | 0 lỗi trong tự đấu (mọi cấp) |
| Ổn định | 1000 ván tự đấu ở thời gian ngắn (trộn các cấp) | 0 treo, 0 quá giờ, 0 nước sai luật |

Chỉ số cần theo dõi: số lượt mô phỏng/giây và RAM đỉnh trên điện thoại thật. Nếu dưới vài trăm lượt/giây thì bot sẽ yếu dù thuật toán đúng.

### 8.2. Cách áp dụng ba quy tắc giảm sức vào cờ vây

* **Không bỏ nước thắng ngay / cứu thua** nghĩa là:
  * **Bất biến cứng, mọi cấp kể cả Siêu việt:** không tự lấp mắt thật để nhóm đang sống tự chết, không tự đánh chết nhóm quân của mình.
  * **Bộ lọc nhiễu, chỉ Dễ đến Giỏi** (không áp dụng cho Cao thủ, Thách đấu, Siêu việt, vì sức mạnh của các cấp đó đến từ tìm kiếm): khi lấy mẫu nước, loại nước tự atari và nước bỏ qua việc cứu nhóm đang bị đe dọa. Dễ và Trung bình lọc thẳng. Khá và Giỏi lọc **có ngoại lệ hy sinh**: giữ lại nước đó nếu giá trị do mạng của cấp ấy đánh giá không kém nước tốt nhất quá ngưỡng `δ` (hy sinh có chủ đích, đe dọa ko, snapback, thang), để bot không bị cứng nhắc.
  * Bộ thế "không được sai" của cờ vây (mục 11.1) chỉ gồm thế có đáp án **không mơ hồ** theo hai ý trên (bất biến cứng); không đưa thế hy sinh hoặc atari vào bộ thế.
* Nhiễu chọn trong nhóm nước có xác suất cao do MCTS đề xuất, không chọn nước ngẫu nhiên toàn bàn.

### 8.3. Cấu hình 7 bot (khởi điểm để thử, phải đo lại)

*Bảng dưới là cho MCTS tự làm, chỉ dùng để thử nghiệm và làm mốc so sánh trong lúc chưa có engine nền. Theo quyết định ở đầu mục này (MCTS thuần không đủ mạnh ở 19×19, không phát hành thang nén), cấu hình này **không được phát hành** trừ khi nó đạt đủ mục 11.1. Cấu hình dự kiến phát hành là mục 8.3a (KataGo).*

Cờ vây giảm sức bằng số lượt mô phỏng và độ ngẫu nhiên khi chọn nước đi. Các cấp trên tính **từ Siêu việt xuống** theo công thức mục 4.3 (`g ≈ 100`); các cấp dưới là điểm bắt đầu cho bàn 19×19:

| Cấp | Số lượt mô phỏng | Nhiễu |
|---|---|---|
| Dễ | Rất thấp (1 đến 10) | Cao |
| Trung bình | Thấp (~10 đến 50) | Vừa |
| Khá | ~50 đến 200 | Thấp |
| Giỏi | ≈ 1/4 lượt của Cao thủ (cách 200 Elo) | Rất thấp |
| Cao thủ | ≈ 1/4 lượt của Thách đấu (cách 200 Elo) | Không |
| Thách đấu | ≈ 1/8 lượt của Siêu việt (cách 300 Elo) | Không |
| Siêu việt | Toàn lực, thời gian `botMillis` (mạng chính sách/giá trị: theo quyết định ở đầu mục này) | Không |

Kiểm tra nhanh:

* Giỏi = Siêu việt ÷ 128. Nếu con số này không lớn hơn Khá ít nhất 16 lần (khoảng cách 400 Elo ứng với `2^(400/100)`), thì phần thiếu phải bù bằng nhiễu ở Khá; nếu thiếu nhiều, Siêu việt chưa đủ mạnh (ý thứ ba ở đầu mục này).
* Ba cấp dưới ở bảng chỉ tăng khoảng ×4 đến ×5 lượt mỗi bậc, tức khoảng 200 đến 230 Elo, thấp hơn nhiều so với khoảng cách mục tiêu 550 / 350 Elo (Trung bình vs Dễ, Khá vs Trung bình). Nếu chỉ dùng lượt thì cần khoảng ×45 và ×11; vì vậy Dễ và Trung bình được làm yếu chủ yếu bằng **nhiễu**, trong giới hạn "không vô lý" (mục 4.2), và con số lượt chỉ chốt sau vòng chỉnh mục 4.3.

### 8.3a. Cấu hình khi dùng KataGo làm engine nền (giả thuyết khởi điểm, chưa đo)

Cờ vây dùng **cả kích thước mạng lẫn số lượt** làm công cụ giảm sức (ngoại lệ có chủ đích của nguyên tắc "cùng một bot", ghi trong hồ sơ game):

| Cấp | Mạng | Lượt tìm (visits) | Chọn nước |
|---|---|---|---|
| Dễ | Nhỏ nhất (6 khối) | 1 | Lấy mẫu theo chính sách, nhiệt độ cao, kèm lớp an toàn |
| Trung bình | Nhỏ nhất | Vài lượt | Lấy mẫu, nhiệt độ vừa |
| Khá | Nhỏ nhất hoặc vừa | Vài chục | Nhiệt độ thấp |
| Giỏi | Vừa (10 khối) | Theo mục 4.3 | Rất thấp |
| Cao thủ | Như Siêu việt | Theo mục 4.3, cách Thách đấu 200 Elo | Không |
| Thách đấu | Như Siêu việt | Theo mục 4.3, cách Siêu việt 300 Elo | Không |
| Siêu việt | Mạnh nhất chạy được trong `botMillis` trên máy thấp nhất | Toàn lực | Không |

**Đo trên CI (11/2026, `ladder go`, KataGo 1.18.2 Eigen, mạng g170 b6/b10):**
* g21 (Dễ b6 1 lượt nhiệt độ 1.6, TB 0.8, Khá b6 12 lượt, Giỏi b10 40, CT b10 160, TĐ b10 480, SV b10 800): ba cặp dưới 100%, Giỏi–Khá 98,1%, CT–Giỏi 95,8%, SV–TĐ 63,9%. Gấp 4 lần số lượt cùng mạng thắng khoảng 96–98%.
* g23 (Dễ/TB/Khá lấy mẫu chính sách b6 nhiệt độ 1.2/0.9/0.6, Giỏi b6 8 lượt, CT b10 40, TĐ b10 200, SV b10 800): TB–Dễ 89,6% (thấp, cần 93–98%), Khá–TB 89,6%, Giỏi–Khá 97,9% (hơi cao), CT–Giỏi 100% ✗, TĐ–CT 95,8% ✗, SV–TĐ 88,9%. Nhảy từ b6 sang b10 cộng gấp 5 lượt là quá xa cho khoảng cách 200 Elo.
* g25 đang đo đường cong từng bước (nhiệt độ, b6 4→8→16 lượt, b6→b10 cùng 16 lượt, b10 16→40→100→200→400→800) để ghép bảng theo khoảng đạt thay vì đoán.

Khi đo trên máy chủ, định nghĩa Siêu việt bằng **số lượt cố định** (lượt/giây bền trên máy thấp nhất × `botMillis`), không bằng thời gian, để kết quả không phụ thuộc phần cứng đo. Số lượt, nhiệt độ và kích thước mạng của từng hàng chỉ chốt sau khi qua vòng chỉnh mục 4.3.

### 8.4. Đối chiếu engine ngoài (tùy chọn)

KataGo hoặc Leela Zero giới hạn theo **số lượt mô phỏng** (visits): 1, 10, 50, 200, 1000, 5000... Cho Siêu việt và vài cấp đấu từng mức, ≥ 100 ván mỗi mức (ván dài, nên chạy song song). Chỉ để báo cáo. Khi engine nền là KataGo thì đối chiếu với chính KataGo ở lượt khác chỉ cho biết thang lượt; muốn có điểm tham chiếu độc lập, dùng engine khác (ví dụ Leela Zero) hoặc bỏ bước này.

---

## 9. Quoridor

Quoridor **không có engine chuẩn được công nhận rộng rãi**, nên 7 bot chỉ đo với nhau (mục 3.4); không có bước đối chiếu ngoài. Tìm kiếm ngày 27/11/2026 chỉ thấy dự án sở thích và bài tập (ví dụ một AI MCTS chạy trên web với các mức 2.500 đến 60.000 lượt); có thể dùng làm đối thủ tham khảo nếu giấy phép cho phép, không dùng làm engine nền. Tự làm theo mục 9.2.

### 9.1. Cổng bắt buộc

| Cổng | Cách kiểm | Mốc |
|---|---|---|
| Luật | Bàn 9×9. Chế độ 2 người, mỗi bên 10 tường: thế đầu có **131 nước hợp lệ** (3 nước quân + 128 vị trí tường). Chế độ 4 người: mỗi bên 5 tường, kiểm thứ tự lượt và đích của từng bên · không được đặt tường chặn kín mọi đường (kiểm bằng BFS) · luật nhảy qua quân đối phương | Khớp tuyệt đối |
| Chiến thuật | Thế "bẫy" mà bên kia nếu đặt tường sai sẽ bị chặn kín; thế chạy đua với độ dài đường đi chênh nhau 1 | Siêu việt chọn đúng nước |
| Ổn định | 1000 ván tự đấu (trộn các cấp) | 0 treo, 0 quá giờ |
| Lợi thế đi trước | Cặp ván đổi bên | Mục 2.5 |

### 9.2. Làm bot Siêu việt mạnh hơn

Hàm đánh giá khởi điểm: hiệu số độ dài đường ngắn nhất tới đích (BFS hoặc A*), cộng chênh lệch số tường còn lại. Số nước mỗi lượt rất lớn, nên cần cắt ứng viên (chỉ xét tường gần quân và đường đi tốt nhất của đối phương). Cải tiến từng bước qua SPRT.

### 9.3. Cấu hình 7 bot: mẫu khởi điểm (cấu hình đã đo và chốt: mục 13.1)

| Cấp | Alpha-beta | Hoặc MCTS (`g ≈ 100`) | Nhiễu |
|---|---|---|---|
| Dễ | Độ sâu 1 | ~50 lượt | Cao |
| Trung bình | Độ sâu 2 | ~200 lượt | Vừa |
| Khá | Độ sâu 3 | ~1000 lượt | Thấp |
| Giỏi | Độ sâu 4 | ≈ 1/4 lượt của Cao thủ (cách 200 Elo) | Rất thấp |
| Cao thủ | Theo mục 4.3: cách Thách đấu 200 Elo | ≈ 1/4 lượt của Thách đấu | Không |
| Thách đấu | Theo mục 4.3: cách Siêu việt 300 Elo | ≈ 1/8 lượt của Siêu việt | Không |
| Siêu việt | Toàn lực, thời gian `botMillis` | Toàn lực | Không |

Lưu ý cho cột MCTS: Dễ, Trung bình, Khá chỉ cách nhau ×4 đến ×5 lượt, tức khoảng 200 đến 230 Elo mỗi bậc, trong khi khoảng cách mục tiêu là 550 và 350 Elo; Khá lên Giỏi cần thêm 400 Elo, tức khoảng ×16 lượt. Chỉ dùng lượt thì phải ×45, ×11, ×16, không khả thi trên điện thoại. Vì vậy các cấp thấp phải bù chủ yếu bằng **nhiễu** (mục 4.2), và mọi con số chỉ chốt sau vòng chỉnh mục 4.3. Cột alpha-beta cũng vậy: mỗi bậc độ sâu chưa chắc cho đủ 550 / 350 / 400 Elo, phải đo.

### 9.4. Trạng thái 11/2026

* **Engine Siêu việt:** alpha-beta PVS, bảng băm Zobrist, killer cho 2 người; paranoid cho 4 người. Tham số ở `object QP` (thử A/B được bằng `new@...`).
* **Các thử nghiệm không có lợi** (A/B với thế mở đầu đa dạng, 240 ván mỗi biến thể, đều quanh 50% so với engine cũ): tường ứng viên chặn mọi đường ngắn nhất, LMR, history, tăng hoặc giảm số tường ứng viên, đổi trọng số tường. Mặc định giữ cách chọn tường cũ. **Bài học đo:** thế mở đầu chỉ gồm 2 nước đi quân cho ra khoảng 9 thế khác nhau, bot giới hạn nút chơi lại y hệt, và kết quả A/B lệch tới 25 điểm phần trăm. Bộ đo giờ dùng 4 nước đi quân cộng mỗi bên một tường ngẫu nhiên.
* **Thang sức nén ở trên:** tìm sâu thêm cho ít Elo (Siêu việt 2 s hơn n300 khoảng 79–81%, hơn n1000 khoảng 75%). Nhiễu có delta từ 2 bước đường đi trở lên làm bot đi lòng vòng, rồi thua vì luật lặp thế của app.
* **Công cụ giảm sức riêng: "quên tường"** (`extra` = % số nước chỉ xét đi quân; lớp an toàn vẫn chặn khi đối phương còn một bước). Đo trên máy (200 cặp, so với `d1`): quên 40% gần như không yếu đi, 60% → 64,5%, 80% → 84%. Gặp đối thủ mạnh (n300) thì gần như không có tác dụng (quên 90% chỉ đưa tỉ lệ từ 68,5% lên 72%), nên Cao thủ dùng thêm nhiễu nhỏ. Nhiễu đặt lên tường còn hại hơn không đặt tường (cấp quên 95% thắng cấp quên 80% có nhiễu), vì vậy các cấp dưới quên tường hoàn toàn và chỉ có nhiễu nhỏ (delta 100) trên nước đi quân. Chuỗi cấp ở mục 13.1 đạt cả 5 cặp kề dưới Siêu việt và 3 cặp cách hai cấp trên máy với 3 bộ seed; xác nhận trên CI ở đợt `g15` (cấp trên) và `g17` (ba cấp dưới sau khi chỉnh Khá p0.5, Trung bình p0.75, Dễ p0.97): cả 6 cặp kề đạt.
* **Engine v3 (28/11/2026):** xem lại các ván Siêu việt thua bot tìm sâu 1 thấy hai lỗi lặp lại: (1) tiêu hết 10 tường trong khoảng 20 nước đầu, mỗi tường chỉ thêm khoảng 1,4 bước cho đối phương, trong khi đối phương giữ tường tới cuối ván thì mỗi tường thêm khoảng 2,7 bước; (2) khi đã hết tường mà đối phương còn nhiều, mọi nước đi quân có điểm như nhau (đối phương đằng nào cũng chặn lại) nên bot đi qua đi lại giữa hai ô hàng chục nước. Sửa: tăng giá trị tường còn giữ (`ww` 75 → 110), cộng thêm `wz` = 150 điểm cho mỗi tường còn lại khi đối phương đã hết tường, và ở gốc (chỉ Siêu việt) chọn nước đi quân tiến về đích nếu nó kém nước tốt nhất không quá `prog` = 20 điểm. A/B trên máy (thế mở đầu đa dạng): vòng 1 `wz150+prog20` 65% (120 ván, 300 ms/nước), vòng 2 thêm `ww110` 70,8% (120 ván), vòng 3 các biến thể quanh đó đều trong sai số; kiểm lại ở 1000 ms/nước: 87,5% so với engine cũ (40 ván). Chuỗi cấp đo lại ở đợt `g22`.
* **Đo lại chuỗi với engine v3 (g22, 2 s/nước):** 6 cặp kề TB–Dễ 94,1%, Khá–TB 88,2%, Giỏi–Khá 88,7%, CT–Giỏi 78,2%, TĐ–CT 92,1%, SV–TĐ 88,4%; 5 cặp cách hai cấp 99,0% / 100% / 98,0% / 94,4% / 92,6%. Engine v3 làm mạnh cả các cấp dùng chung hàm đánh giá, nên Siêu việt vs Cao thủ từ 73% lên 92,6% (đạt). Còn một cặp vượt trần: Thách đấu vs Cao thủ 92,1% (trần 88%), đang thử làm mạnh Cao thủ ở g24 (hai biến thể: p0.10 x150 e25 và p0.05 x100 e10).
* **Hạn chế trước engine v3:** Siêu việt chỉ thắng Cao thủ khoảng 73% (cần ≥ 80% cho cặp cách hai cấp), dù thắng Thách đấu 80% và Thách đấu thắng Cao thủ 77%. Đo trên máy ở 300 ms/nước, Siêu việt thắng mọi biến thể Cao thủ tìm sâu 1 (không nhiễu, nhiễu p0.2 đến p0.6, quên tường 0 đến 60%) chỉ 72–78%: làm Cao thủ yếu đi không giúp được. Đây là điểm yếu của engine Siêu việt trước lối chơi nông, cần cải thiện engine (đánh giá thế đua về đích, đặt tường phòng thủ) chứ không phải cấu hình cấp.

---

## 10. Wall Go

Wall Go cũng **không có engine chuẩn**, nên 7 bot chỉ đo với nhau. Tìm kiếm ngày 27/11/2026 thấy một bản remake giấy phép MIT có AI yếu và một mã nghiên cứu (WallZero) chưa rõ giấy phép; chưa đủ làm engine nền. Tự làm theo mục 10.2.

### 10.1. Cổng bắt buộc

* Chốt **một bộ luật** (cách vây vùng, cách tính điểm, điều kiện kết thúc) và ghi vào `docs/how-to-play/`, vì bot mạnh sẽ khai thác mọi chỗ mập mờ của luật.
* Kiểm thử luật bằng các thế viết tay và cho hai bản cài đặt độc lập đấu nhau (khi game chưa có bảng perft).
* Ổn định: 1000 ván tự đấu (trộn các cấp), 0 treo, 0 quá giờ, 0 nước sai luật.
* Lợi thế đi trước: cặp ván đổi bên (mục 2.5).

### 10.2. Làm bot Siêu việt mạnh hơn

Hợp với MCTS hoặc alpha-beta với hàm đánh giá diện tích vây được. Cải tiến từng bước qua SPRT.

### 10.3. Cấu hình 7 bot: mẫu khởi điểm (cấu hình đã đo và chốt: mục 13.1)

Dùng cùng bảng với Quoridor (mục 9.3): độ sâu 1 đến 4 cho Dễ đến Giỏi, rồi giới hạn nút hoặc lượt cho Cao thủ và Thách đấu theo công thức ở mục 4.3 (đo `g` riêng cho Wall Go), Siêu việt toàn lực. Số cụ thể phải đo lại riêng cho Wall Go vì độ rộng và độ dài ván khác Quoridor.

### 10.4. Trạng thái 11/2026

* **Engine Siêu việt (2 người):** negamax PVS đi/hoàn tác trên một bàn, bảng băm Zobrist, killer. Ở nút sát lá, điểm sắp xếp (đánh giá tĩnh của thế con) dùng luôn làm giá trị lá, không đánh giá lại và không cắt nước. Hàm đánh giá nhanh `score2` không cấp phát bộ nhớ. Độ sâu ở 1 s tăng từ 4 lên 5. 3-4 người vẫn dùng paranoid như cũ.
* **A/B trên CI:** engine mới hơn engine cũ +124 Elo ở 1 s/nước (408 ván, 67%).
* **7 cấp theo độ sâu và nhiễu** (không đổi theo máy): Siêu việt chỉ duyệt vài nghìn nút mỗi nước vì mỗi nút đánh giá mọi nước con, nên giới hạn nút không phù hợp. Kết quả đo trên CI ở mục 13.2.

---

## 11. Nghiệm thu một game

### 11.1. Danh sách nghiệm thu

Một game được coi là **xong** khi mọi mục dưới đây đều đạt:

- [ ] Luật qua perft hoặc bộ kiểm thử tương đương, nằm trong `SelfCheck.kt`, chạy ở CI.
- [ ] **Nguồn bot:** nếu dùng engine nền, cổng giấy phép và cổng kỹ thuật (mục 4.0) đạt, hồ sơ ghi tên, phiên bản hoặc commit, giấy phép của mã và của dữ liệu, và ứng dụng có tệp giấy phép/NOTICE; nếu tự làm, hồ sơ ghi "không có engine đủ giấy phép" kèm ngày tra cứu.
- [ ] Mọi cổng chiến thuật của game (mục 5 đến 10) qua với bot Siêu việt. Mốc chốt từ lần đo đầu chỉ khóa hồi quy, nên phải kèm **sàn tuyệt đối** do bạn đặt trước trong hồ sơ game (mốc đầu tệ mà đem chốt thì khóa luôn cái tệ).
- [ ] Bộ thế "không được sai" (thế có nước thắng ngay hoặc nước cứu thua trong một nước; **≥ 30 thế mỗi game**, mỗi thế có đáp án đúng không mơ hồ; cờ vây theo phạm vi ở mục 8.2) qua **100% với cả 7 cấp**.
- [ ] 1000 ván tự đấu trộn các cấp: 0 treo, 0 quá giờ, 0 nước sai luật.
- [ ] Game có chế độ 3 đến 4 người: hai kiểm tra ổn định và thứ tự cấp ở mục 12.1 đạt cho mỗi số người hỗ trợ.
- [ ] Mọi ván đều kết thúc: luật lặp thế hoặc số nước tối đa kèm kết quả khi chạm trần (hòa hoặc theo điểm) được ghi trong hồ sơ game; báo cáo tỉ lệ ván chạm trần và ảnh hưởng lên điểm.
- [ ] Tỉ lệ chạm trần thời gian của các cấp giới hạn theo nút dưới 1% trên máy thấp nhất hỗ trợ (mục 3.2).
- [ ] Hệ số chậm điện thoại `k` đã đo (mục 3.2); Siêu việt được đo ở thời gian đã chuẩn hóa.
- [ ] **7 cấu hình đã chốt**, ghi trong hồ sơ game và trong một bảng cấu hình duy nhất trong mã (mục 4.4).
- [ ] **6 cặp kề nhau** (mục 1.3): mỗi cặp ≥ 200 ván (Trung bình vs Dễ: ≥ 400), kết luận `đạt` theo quy tắc mục 3.4 (`pair_verdict`).
- [ ] **5 cặp cách hai cấp** (mục 3.4): mỗi cặp ≥ 100 ván, kết luận `đạt` theo `min_gap_verdict`; 3 cặp smoke 20 ván đạt.
- [ ] Đã xem tay vài ván mỗi cấp: không có nước vô lý (nhất là Dễ và Trung bình).
- [ ] Game có lợi thế đi trước: tỉ lệ điểm khi đi trước và đi sau được ghi riêng cho từng cặp.
- [ ] Trên điện thoại thật: Siêu việt có thời gian phản hồi ở phân vị 99 (`p99`) ≤ 1,10 × `botMillis`; các cấp còn lại có `p99` ≤ `botMillis`; RAM đỉnh không vượt giới hạn của dự án; sau 20 ván liên tiếp số nút/giây không giảm quá 20% (nóng máy).
- [ ] Báo cáo đo đã lưu cùng commit.

Chỉ cần **một mục chưa đạt** thì game chưa xong. Thêm game mới không buộc đo lại game đã xong; nhưng game đã xong phải đo lại khi Siêu việt đổi (mục 4.3), khi engine nền đổi phiên bản hoặc mạng, khi luật hoặc hàm đánh giá dùng chung đổi, hoặc khi `k` đổi (mục 3.2). Mỗi lần đo lại ghi vào bảng theo dõi cùng commit.

### 11.2. Mẫu báo cáo (điền cho mỗi lần đo)

```
Game: ...                Commit bot: ...        Ngày: ...
Phần cứng: ...           Hệ số chậm điện thoại k: ...
Thời gian Siêu việt (đã chuẩn hóa): ... ms/nước

Cấu hình 7 cấp (mục 4.4): Dễ ..., Trung bình ..., Khá ..., Giỏi ..., Cao thủ ..., Thách đấu ..., Siêu việt ...

Cặp kề (cấp cao vs cấp thấp) | số ván | thắng/hòa/thua | S | khoảng đạt | kết quả
  Trung bình vs Dễ:       ...
  Khá vs Trung bình:      ...
  Giỏi vs Khá:            ...
  Cao thủ vs Giỏi:        ...
  Thách đấu vs Cao thủ:   ...
  Siêu việt vs Thách đấu: ...

Cặp cách hai cấp (ngưỡng tối thiểu ở mục 1.3):
  Khá vs Dễ: ...   Giỏi vs Trung bình: ...   Cao thủ vs Khá: ...
  Thách đấu vs Giỏi: ...   Siêu việt vs Cao thủ: ...

Bảng Elo tổng hợp (Ordo/BayesElo từ các cặp đã đo, mục 2.4): Dễ ... Trung bình ... Khá ... Giỏi ... Cao thủ ... Thách đấu ... Siêu việt ...
Tỉ lệ điểm khi đi trước / đi sau: ...
Cổng chiến thuật: ... qua / ... không qua      Bộ thế "không được sai": .../... 
Đối chiếu engine ngoài (tùy chọn): ...
Phản hồi điện thoại: p99 = ... ms, RAM đỉnh = ... MB, nps giảm = ...%, tỉ lệ chạm trần thời gian = ...%
Ba cặp smoke (Siêu việt vs Dễ, Siêu việt vs Khá, Thách đấu vs Dễ): ...
Ghi chú: ...
```

### 11.3. Không có lệch chuẩn

Quyết định của chủ sở hữu dự án: **không phát hành thang nén**. Game không đạt đủ mọi mục ở 11.1 thì chưa phát hành. Cách sửa là tăng sức Siêu việt (đổi engine nền, mạng, thuật toán, cấu hình máy tối thiểu), không phải hạ yêu cầu. Muốn đổi quyết định này thì sửa mục này **trước**, không đo xong rồi mới hạ chuẩn.

### 11.4. Kiểm thử nhẹ cho commit thường

Nghiệm thu đầy đủ (mục 11.1) chỉ chạy khi phát hành hoặc khi đổi điều kiện của nó (Siêu việt, hàm đánh giá hoặc luật dùng chung, `k`). Với commit thường, chạy hằng đêm: bộ thế chiến thuật, bộ thế "không được sai" và khoảng 40 ván mỗi cặp kề để bắt lệch lớn. Kết quả 40 ván chỉ để cảnh báo, không dùng để chốt cấu hình hay nghiệm thu.

---

## 12. Thêm game mới: hồ sơ game

Trước khi viết bot cho một game mới, điền **hồ sơ game** (mục 12.2). Hồ sơ buộc bạn trả lời các câu hỏi về cách đo và về 7 cấp từ đầu, nên khi bot viết xong thì chỉ việc đo.

### 12.1. Game nào áp dụng được

Tài liệu áp dụng cho game **thông tin đầy đủ có chế độ hai người chơi**, vì khi đó 7 bot đấu với nhau được và Elo có ý nghĩa. Các trường hợp khác:

* **Game có may rủi hoặc thông tin ẩn:** vẫn đấu bot với bot được, nhưng phương sai lớn, nên **nhân 3 đến 5 lần số ván** ở mọi bước đo.
* **Game đã giải hoặc gần giải** (ví dụ Connect 4): bot Siêu việt chơi hoàn hảo. Các cấp dưới vẫn xếp theo khoảng đạt ở mục 1.3 như bình thường, chỉ cần bảo đảm các cấp thấp được giảm sức bằng nhiễu có kiểm soát, vì giới hạn độ sâu một mình không đủ làm yếu bot ở game nhỏ.
* **Chế độ 3 đến 4 người** (Quoridor 4 người, Wall Go 3 đến 4 người): Elo và khoảng đạt ở mục 1.3 là cho hai người, không áp dụng trực tiếp. Bảy cấp được chỉnh và nghiệm thu ở **chế độ 2 người**; chế độ nhiều người dùng lại đúng 7 cấu hình đó (chỉ khác `botMillis` nếu hồ sơ game ghi), không chỉnh riêng. Chế độ nhiều người chỉ cần thêm hai kiểm tra:
  * **Ổn định:** 1000 ván tự đấu ở mỗi số người hỗ trợ, trộn các cấp: 0 treo, 0 quá giờ, 0 nước sai luật.
  * **Thứ tự cấp:** mỗi số người, ≥ 100 ván gồm một bot cấp cao và các bot còn lại cùng một cấp thấp hơn, đổi vị trí ngồi luân phiên. Cấp cao phải thắng nhiều hơn rõ rệt tỉ lệ ngẫu nhiên `1/n` (`n` là số người), kiểm cho các cặp kề. Đây là kiểm tra thứ tự, không phải đo khoảng cách.
* **Game một người chơi** (đố, ghép hình, Sudoku...) **không thuộc phạm vi tài liệu này**: không có đối thủ nên không cần bot 7 cấp.

### 12.2. Mẫu hồ sơ game

Lưu cạnh mã game (ví dụ `docs/games/<id>.md`):

```
Game: ...
Luật chốt: ... (biến thể, hòa, lặp thế, kết thúc)
Kiểm thử luật: perft / thế viết tay / hai bản cài đặt độc lập (nêu rõ)
Bộ thế chiến thuật: ... (n=...; sàn tuyệt đối: ...)    Bộ thế "không được sai": ... (n=...)
Ngân sách thời gian botMillis (Siêu việt): ...    Hệ số chậm điện thoại k: ...
Lợi thế đi trước: ... (cách xử lý: hoán đổi / thế mở đầu cân bằng)
Nguồn bot: engine nền = <tên, phiên bản/commit, giấy phép của mã, giấy phép của dữ liệu/mạng, ngày tra cứu, đường dẫn> | tự làm (lý do, ngày tra cứu)
Engine ngoài để đối chiếu (nếu có): ...
Nguồn thế mở đầu: ... (≥ số cặp ván; cách sinh và cách lọc cân bằng)
Điều kiện kết thúc: lặp thế / số nước tối đa = ... ; kết quả khi chạm trần = ...
win_in_1 / lose_in_1: ... (hoặc "không áp dụng": lý do)
g theo vùng nút: gần Khá ..., gần Giỏi/Cao thủ ..., gần Thách đấu/Siêu việt ...
Ngân sách máy dự kiến (giờ CPU): 6 cặp kề ..., 5 cặp cách hai cấp ..., đo g ...
Trần SPRT: ...    Ngưỡng xử thắng sớm (nếu có): ...

7 cấp (mỗi cấp một dòng, theo mẫu mục 4.4):
  Dễ: ...   Trung bình: ...   Khá: ...   Giỏi: ...
  Cao thủ: ...   Thách đấu: ...   Siêu việt: ...

Kết quả 6 cặp kề: ... (mục 11.2)
Kết quả 5 cặp cách hai cấp + 3 cặp smoke: ...
```

### 12.3. Quy trình thêm một game

1. Tra cứu engine có giấy phép thương mại (mục 4.0) rồi điền hồ sơ (mục 12.2).
2. Cài `BoardGame` (`Game.kt`), viết luật, `make/unmake`, kiểm thử luật (mục 5 đến 10 hoặc A.1).
3. Tự làm: viết bot bản đầu (tìm kiếm cơ bản). Dùng engine nền: nối engine qua lớp bọc và qua cổng kỹ thuật (mục 4.0).
4. Cải tiến thành bot Siêu việt (với engine nền: chọn phiên bản, mạng và tham số tốt nhất), mỗi lần một thay đổi qua SPRT (mục 2.6).
5. Chốt Siêu việt, rồi làm sáu cấp còn lại theo mục 4.3.
6. Đo 6 cặp kề và 5 cặp cách hai cấp (mục 3.4).
7. Khi báo cáo đạt: ghi vào bảng theo dõi (mục 13) cùng commit.
8. Thêm kiểm thử vào `SelfCheck.kt` để CI bắt lỗi hồi quy.

---

## 13. Bảng theo dõi

Mỗi lần chốt cấu hình hoặc đo xong một cặp, cập nhật bảng cùng commit. Số liệu dưới đây đo trên GitHub Actions (mục 3.3), 11/2026. Kết quả từng ván nằm ở `tools/bot-bench/results/<đợt>.txt`, báo cáo ở `docs/bot-results/`.

### 13.1. Cấu hình đã chốt

Cú pháp: `d<độ sâu>` / `n<số nút>`, `p` xác suất nhiễu, `x` delta (đơn vị điểm của engine), `e` tham số riêng (caro: độ sâu VCF; Quoridor: % nước "quên tường"). Siêu việt: toàn lực trong `botMillis`. Nguồn: `core/src/main/kotlin/com/fws/playmind/core/BotLevels.kt`.

| Game | Dễ | Trung bình | Khá | Giỏi | Cao thủ | Thách đấu | Siêu việt |
|---|---|---|---|---|---|---|---|
| Cờ vua | d1 p1.0 x500 | d1 p0.6 x200 | d2 p0.5 x150 | n700 p0.05 x20 | n4000 | n15000 | toàn lực, 3 luồng |
| Cờ tướng | d1 p0.9 x450 | d1 p0.6 x250 | d2 p0.5 x180 | n300 p0.05 x25 | n3000 | n16000 | toàn lực, 3 luồng |
| Cờ caro | d1, nhiễu rộng p0.5 | d1, nhiễu rộng p0.15 | d1 p0.2 x800 e3 | n250 p0.03 x200 e12 | n1000 e24 | n4000 e24 | toàn lực e24 |
| Cờ vây 19×19 | chưa chốt | chưa chốt | chưa chốt | chưa chốt | chưa chốt | chưa chốt | chưa chốt |
| Quoridor | d1 p0.97 x100 e100 | d1 p0.75 x100 e100 | d1 p0.5 x100 e100 | d1 e80 | d1 p0.1 x150 e25 (g24) | n300 | toàn lực |
| Cờ cối xay (khởi điểm, chờ g26) | d1 p0.9 x300 | d1 p0.5 x150 | d2 p0.3 x80 | d3 | d4 | n60000 | toàn lực |
| Lục giác trí nhớ (nhớ đúng / tìm / nhớ bộ đã gọi) | 45% / 34 s / 35% | 60% / 30 s / 60% | 72% / 22 s / 70% | 82% / 16 s / 80% | 86% / 13,5 s / 85% | 90% / 11,5 s / 90% | 93% / 10 s / 95% |
| Wall Go | d1 p1.0 x700 | d1 p0.9 x400 | d1 p0.8 x250 | d1 | d2 | d3 | toàn lực |
| Công thức cao thấp (tạm) | MC 4 mẫu p0.7 | MC 8 p0.4 | MC 16 p0.2 | MC 40 p0.08 | MC 100 p0.03 | MC 200 | MC 400 |
| Lãnh địa đồng cỏ (tạm) | d1 p0.75 | d1 p0.45 | d1 p0.25 | d1 p0.15 | d1 p0.08 | d1 p0.03 | toàn lực (một lớp) |

### 13.2. Tỉ lệ điểm của cấp cao trong 6 cặp kề nhau

Tỉ lệ điểm đo được (khoảng 200 ván; 400 ván cho Trung bình vs Dễ), trong ngoặc là đợt đo. Dưới tên cặp là khoảng đạt. Dấu ✗ là chưa đạt.

| Game | Trung bình vs Dễ (93–98%) | Khá vs Trung bình (81–95%) | Giỏi vs Khá (85–96%) | Cao thủ vs Giỏi (64–88%) | Thách đấu vs Cao thủ (64–88%) | Siêu việt vs Thách đấu (76–93%) |
|---|---|---|---|---|---|---|
| Cờ vua | 97,3% (g16) | 83,8% (g16) | 92,2% (g16) | 72,3% (g16) | 67,4% (g16) | 80,6% (g16) |
| Cờ tướng | 96,9% (g12) | 87,3% (g12) | 90,7% (g12) | 68,4% (g13) | 73,5% (g13) | 83,6% (g13) |
| Cờ caro | 94,1% (g11) | 84,3% (g12) | 89,2% (g12) | 81,4% (g11) | 72,1% (g11) | 76,9% (g11) |
| Cờ vây 19×19 | chưa đo | chưa đo | chưa đo | chưa đo | chưa đo | chưa đo |
| Quoridor (engine v3) | 94,1% (g22) | 88,2% (g22) | 88,7% (g22) | 78,2% (g22) | 82,4% (g24, Cao thủ mới) | 88,4% (g22) |
| Wall Go | 93,3% (g12) | 91,7% (g15) | 88,0% (g15, 408 ván) | 81,9% (g12) | 86,5% (g12) | 89,8% (g12) |
| Công thức cao thấp (8 người, máy tính) | 95,0% | 93,3% | 78,3% | 66,7% ✗ | 48,3% ✗ | 55,0% ✗ |
| Lãnh địa đồng cỏ (8 người, máy tính) | 74,0% ✗ | 93,0% | 49,5% ✗ | 60,5% ✗ | 55,0% ✗ | 53,0% ✗ |

Cặp cách hai cấp, mức tối thiểu ghi dưới tên cặp:

| Game | Khá vs Dễ (≥ 97%) | Giỏi vs Trung bình (≥ 94%) | Cao thủ vs Khá (≥ 88%) | Thách đấu vs Giỏi (≥ 70%) | Siêu việt vs Cao thủ (≥ 80%) |
|---|---|---|---|---|---|
| Cờ vua | 99,3% (g18, 408 ván) | 96,1% (g16) | 100% (g16) | 89,7% (g16) | 95,8% (g16) |
| Cờ tướng | 98,0% (g12) | 95,1% (g12) | 98,0% (g15) | 93,1% (g13) | 94,4% (g13) |
| Cờ caro | 100% (g11) | 98,0% (g12) | 96,1% (g12) | 87,3% (g11) | 97,2% (g11) |
| Quoridor (engine v3) | 99,0% (g22) | 100% (g22) | 98,0% (g22) | 94,4% (g22) | 92,6% (g22) |
| Wall Go | 100% (g12) | 99,0% (g13) | 99,0% (g13) | 98,0% (g12) | 96,3% (g12) |

### 13.3. Các hạng mục còn lại

| Game | Luật | Cổng chiến thuật | Bộ thế không được sai (7 cấp) | Khoảng cách tối thiểu | 1000 ván tự đấu | `k` đã đo | Điện thoại (p99, RAM, nóng máy) | Nguồn bot | Kết luận |
|---|---|---|---|---|---|---|---|---|---|
| Cờ vua | đạt (`SelfCheck`) | đạt (`SelfCheck`) | đạt (lớp an toàn) | đạt (g16, g18) | chưa | chưa | chưa | tự làm; Stockfish `UCI_Elo` 2800: 58,3% (chess-t4) | còn phần đo trên điện thoại |
| Cờ tướng | đạt | đạt | đạt | đạt (g12, g13); đo lại với trọng số xiangqi-t2 ở g26 | chưa | chưa | chưa | tự làm; trọng số xiangqi-t2 +38 Elo (g22 + g24, 816 ván); Pikafish 20k nút: 12,5% (trọng số cũ) | chờ g26, còn phần đo trên điện thoại |
| Cờ caro | đạt | đạt (VCF, phòng VCF) | đạt | đạt (g11, g12) | chưa | chưa | chưa | tự làm; +232 Elo so với engine cũ | còn phần đo trên điện thoại |
| Cờ vây 19×19 | đạt | chưa | chưa | chưa | chưa | chưa | chưa | KataGo (mục 8.3a) | chưa xong |
| Quoridor | đạt | đạt | đạt | đạt (engine v3, g22) | chưa | chưa | chưa | tự làm; engine v3 +340 Elo so với engine cũ (mục 9.4) | còn phần đo trên điện thoại |
| Wall Go | đạt | đạt | đạt | đạt (g12, g13, g15) | chưa | chưa | chưa | tự làm; +124 Elo so với engine cũ | còn phần đo trên điện thoại |
| Công thức cao thấp | đạt (`SelfCheck`) | chưa | chưa | chưa | chưa | chưa | chưa | tự làm, Monte Carlo trên thẻ chưa thấy | chưa xong: Cao thủ trở lên gần như ngang nhau (mục 13.5) |
| Lãnh địa đồng cỏ | đạt (`SelfCheck`) | chưa | chưa | chưa | chưa | chưa | chưa | tự làm, đánh giá một lớp | chưa xong: cấp Khá trở lên gần như ngang nhau, cần bot có tìm kiếm (mục 13.5) |
| Cờ cối xay | đạt (`SelfCheck`) | chưa | chưa | chờ g26 | chưa | chưa | chưa | tự làm, alpha-beta + TT + tìm tiếp nước ăn quân | chờ g26 |
| Lục giác trí nhớ | đạt (`SelfCheck`) | không áp dụng | không áp dụng | đạt trên máy (150 cặp), chờ g26 | chưa | không áp dụng | không áp dụng | tự làm, mô phỏng trí nhớ và tốc độ | chờ g26 |
| *(game mới)* | | | | | | | | | |

### 13.4. Chuẩn chung: cờ vua

Mọi game từ 2 người trở lên có 7 cấp đều đo trên GitHub Actions và dùng **chung một thang với cờ vua**: cấp cao phải đạt
cùng khoảng tỉ lệ điểm với cấp thấp liền kề như cờ vua (bảng 13.2) và cùng mức tối thiểu ở các cặp cách hai cấp. Vì tỉ lệ
điểm quy ra cùng một khoảng cách Elo (mục 2), một người thắng được cấp Giỏi ở cờ vua sẽ thấy cấp Giỏi ở mọi game khác khó
tương đương. Game nhiều người (Công thức cao thấp, Lãnh địa đồng cỏ) đo nửa ghế mỗi cấp (mục 13.5) nên 50% vẫn là ngang
sức. Lục giác trí nhớ (bấm chuông thời gian thực) đo bằng mô phỏng bot đấu bot cùng mã với app.

### 13.5. Lãnh địa đồng cỏ: đo nhiều người

`ladder meadow <A> <B> <cặp> <ms> <seed> <số người>`: nửa ghế cấp A, nửa ghế cấp B xen kẽ, đổi ghế ở ván thứ hai của cặp.
A được 1 điểm khi ghế A tốt nhất hơn ghế B tốt nhất (điểm hình vuông, rồi diện tích), nên hai cấp ngang nhau cho 50% và
dùng chung ngưỡng với các game hai bên. Mỗi ván mất khoảng 1 giây trên máy tính, chạy được ngay trên máy (mã tự làm).

Kết quả 11/2026, 100 ván mỗi cặp (4 người / 8 người): 1v0 59% / 74%, 2v1 91% / 93%, 3v2 58% / 49,5%, 4v3 61% / 60,5%,
5v4 53% / 55%, 6v5 51% / 53%, 6v4 57% / 52%. Giảm nhiễu từ Khá trở lên gần như không đổi sức mạnh: bot đánh giá một lớp
đã chơi gần mức trần của chính nó, còn các tính năng theo cấp (phiếu đổi gạch, cản người dẫn đầu, nhìn mảnh kế tiếp) chưa
cho thêm điểm. Việc tiếp theo: tìm kiếm nhiều lớp trên hàng chờ công khai (các mảnh kế tiếp của mình và của đối thủ),
rồi chia 7 cấp theo độ sâu và số nước xét.

Công thức cao thấp dùng cùng lệnh (`ladder hilo ...`): A được 1 điểm khi người còn chip cuối cùng ngồi ghế A. Kết quả 11/2026,
60 ván mỗi cặp (4 người / 8 người): 1v0 92% / 95%, 2v1 78% / 93%, 3v2 72% / 78%, 4v3 60% / 67%, 5v4 55% / 48%, 6v5 50% / 55%,
6v4 50% / 50%. Ba cấp dưới tách rõ nhờ nhiễu và số mẫu; từ 40 mẫu trở lên thêm mẫu không thêm sức mạnh, phần giới hạn là
chính sách cược (ngưỡng theo phần pot kỳ vọng, chưa mô hình hóa đối thủ chọn cửa nào). Việc tiếp theo: ước lượng cửa đối thủ
từ thẻ ngửa, cược theo vị trí và kích cỡ pot, rồi chia lại 7 cấp.

---

## Phụ lục A: kỹ thuật nền đã dùng trong dự án

Phụ lục này áp dụng cho game **tự làm bot** (mục 4.0). Game dùng engine nền chỉ cần A.5 (tích hợp trên điện thoại) và A.6 (lỗi thường gặp) ở phần liên quan.

### A.1. Bốn trụ cột

1. **Luật đúng tuyệt đối.** Kiểm chứng bằng perft trước khi viết dòng AI nào.
2. **Tìm kiếm sâu và thông minh.** Alpha-beta chỉ là điểm xuất phát; thứ tự duyệt nước đi và cắt tỉa có kiểm soát mới tạo ra sức mạnh.
3. **Đánh giá có tri thức.** Vật chất, vị trí, cơ động, cấu trúc, an toàn, giai đoạn ván.
4. **Đo lường, không đoán.** Mỗi thay đổi phải qua SPRT; mỗi cấp phải đo bằng trận đấu với cấp kề (mục 3.4); bản Siêu việt có thể đối chiếu thêm engine ngoài (mục 3.5).

### A.2. Khung tìm kiếm (game hai người, thông tin đầy đủ)

```
searchRoot:  đào sâu dần; aspiration window quanh điểm vòng trước;
             nước tốt nhất vòng trước xếp đầu; PVS;
             dừng khi hết thời gian mềm (~55% ngân sách) hoặc thấy chiếu hết

search(depth, alpha, beta):
    hết giờ -> abort;  hòa do lặp thế / luật 50 nước -> điểm hòa (0 trừ contempt, A.4 mục 7)
    bị chiếu -> depth++;  depth <= 0 -> quiesce()
    tra bảng băm; reverse futility; null move; IID
    sắp xếp nước: bảng băm > ăn quân SEE>=0 > phong cấp > killer > history > ăn lỗ
    cho mỗi nước: futility / late-move pruning, LMR (tìm lại nếu vượt alpha), PVS
    cắt beta: cập nhật killer + history
    lưu bảng băm

quiesce: chỉ nước ăn quân (hoặc mọi nước nếu bị chiếu); stand-pat, delta pruning, bỏ nước ăn lỗ theo SEE
```

Guard bắt buộc: null move không dùng khi bị chiếu, không dùng hai lần liên tiếp, và tắt ở tàn cuộc dễ zugzwang; quiesce phải có giới hạn ply (cờ tướng cần cắt chiếu dài lặp vô hạn trong quiesce). Tham số cờ tướng tính theo thang điểm quân của cờ tướng, không mượn nguyên từ cờ vua; SEE với pháo cần logic riêng (ăn qua ngòi, x-ray).

Tham số khởi điểm (tinh chỉnh bằng SPRT):

| Tham số | Cờ vua | Cờ tướng |
|---|---|---|
| Bảng băm | 2^19 mục | 2^19 mục |
| Null-move R | 2 + depth/4 + min(2, (eval−beta)/200) | 2 + depth/4 |
| Reverse futility | depth ≤ 6, 90·depth | depth ≤ 5, 110·depth |
| Futility | depth ≤ 3, 110·depth + 40 | depth ≤ 3, 120·depth + 60 |
| Late-move pruning | quiet thứ > 6 + depth² (depth ≤ 4) | quiet thứ > 8 + depth² |
| LMR | 0,7 + ln(d)·ln(m)/2,3 | 0,6 + ln(d)·ln(m)/2,4 |
| Aspiration | ±28, nới ×3 | ±30, nới ×3 |

### A.3. Đánh giá tapered (cờ vua)

`điểm = (trungCuộc·giaiĐoạn + tànCuộc·(24 − giaiĐoạn)) / 24`, với vật chất và bảng điểm vị trí theo hai giai đoạn, cơ động, cấu trúc tốt (tốt thông, cô lập, chồng), xe trên cột mở và hàng 7, cặp tượng, an toàn vua, tàn cuộc dồn vua ra mép.

### A.4. Bài học kỹ thuật

1. **Tính năng "thông minh hơn" có thể làm bot yếu đi.** Một module đe dọa chạy quá lâu hoặc có lỗi (ví dụ VCT ở caro) có thể làm bản đầy đủ tính năng thua bản đơn giản; chỉ khi bật tắt từng tính năng và đo A/B mới tìm ra thủ phạm.
2. **Ngân sách thời gian là tài nguyên.** Giới hạn mỗi module đe dọa ≤ 10–20% thời gian và cho thoát sớm.
3. **Kiểm tra giờ phải đủ dày.** Nút nặng kiểm tra `nanoTime` mỗi 16 nút; nút nhẹ mỗi 1024 nút.
4. **"Hết giờ" không bao giờ nghĩa là "an toàn".** Theo dõi cờ "đã cạn ngân sách" riêng.
5. **Không quét toàn bàn ở mỗi nút.** Dùng danh sách cửa sổ hoạt động cập nhật tăng dần.
6. **Mảng đệm dùng chung giữa các hàm đệ quy lồng nhau sẽ bị ghi đè.** Mỗi hàm có mảng riêng theo độ sâu.
7. **Hòa do lặp thế cần contempt:** điểm hòa hơi âm cho bot (15 cp cờ vua, 30 cp cờ tướng).
8. **Cẩn thận `pkill -f`:** nó khớp cả dòng lệnh của shell chạy nó; neo mẫu bằng `pgrep -f "^java -jar x.jar"`.

### A.5. Tích hợp trên điện thoại

* Thời gian mỗi nước: cờ vua và cờ tướng 4,5 s, caro 3,5 s (`Catalog.botMillis`). Cờ vây, Quoridor, Wall Go: chốt `botMillis` trong hồ sơ game (mục 12.2).
* Chạy trên luồng nền (`Dispatchers.Default`) với bản sao của bàn cờ (`copyGame()`).
* Không cấp phát trong vòng lặp nóng. Một luồng: bảng băm riêng, không cần khóa. Lazy SMP (mục 5.2) cần bảng băm không khóa (ví dụ lưu key XOR data, chấp nhận mục hỏng hiếm và kiểm hợp lệ nước lấy từ bảng); `synchronized` trên đường nóng là nút thắt.
* `BotNames.humanDelay()` tạo nhịp đi giống người.

### A.6. Lỗi thường gặp

| Triệu chứng | Nguyên nhân thường gặp |
|---|---|
| Bot đi nước ngu ở độ sâu cao | `make/unmake` không đối xứng; chạy perft và so băm tăng dần với băm tính lại |
| Bot treo hoặc quá giờ | Quên kiểm tra giờ trong `quiesce`, hoặc vòng aspiration không thoát |
| Bot chấp nhận hòa khi đang hơn | Điểm hòa = 0 nhưng đánh giá hơi âm; hoặc phát hiện lặp thế quá rộng |
| Bot mạnh lên rồi tụt sau một thay đổi | Thiếu SPRT; luôn giữ bản cũ để đấu |
| Kết quả không lặp lại | Bảng băm dùng chung giữa các ván; xóa trước mỗi ván khi đo |
