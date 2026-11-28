# Gói trả phí, kim cương và quảng cáo

## Quy tắc hiện hành

| | Miễn phí | Gói tháng (1 USD/tháng) | Gói năm (10 USD/năm) |
|---|---|---|---|
| Lượt chơi miễn phí / game / ngày | 1 | 3 | không giới hạn |
| Quảng cáo biểu ngữ | có | **không** | **không** |
| Quảng cáo mở app | có | **không** | **không** |
| Cấp độ máy | mở dần (thắng cấp L mở cấp L + 1) | mở dần | **mở ngay cả 7 cấp** |
| Kim cương (tốn, thưởng, điểm danh) | có | có | **bỏ hẳn**: không hiện ở đâu trong app |

App chỉ có 2 loại quảng cáo: biểu ngữ (`ADMOB_BANNER`) và mở app (`ADMOB_APP_OPEN`). Không có quảng cáo xen kẽ hay quảng cáo thưởng.

Hết lượt miễn phí: mỗi ván tốn 1 kim cương. Thắng +1 kim cương, điểm danh +1, chuỗi thắng và thưởng gắn bó như cũ (xem trong app: Trợ giúp).

**Chơi với bạn bè** (phòng gần nhau / internet, kể cả khi bot vào ghế trống): mỗi ván mỗi người -1 kim cương, không dùng lượt miễn phí; người thắng +1. Gói tháng hoặc gói năm không mất kim cương (`WalletRepo.canStart/startMatch(room = true)`).
Điểm danh +1 mỗi ngày; điểm danh liên tục 30 ngày thì mỗi lần tiếp theo +2 (gói tháng: +4).

**Gói năm bỏ hẳn kim cương** (`WalletRepo.noDiamonds()`): không tốn, không thưởng, không điểm danh; ô kim cương ở trang chủ, chip kim cương, nhãn lượt miễn phí, gói tháng và mục mua kim cương trong cửa hàng đều ẩn. Kim cương không bao giờ tăng (kể cả `addDiamonds`; giao dịch kim cương còn treo không được tiêu thụ). Số kim cương cũ vẫn được giữ trong ví; chỉ khi gói năm bị hủy / hết hạn thì các gói khác hiện lại và kim cương hoạt động lại. Gói năm cũng mở ngay cả 7 cấp độ máy (`Prefs.allLevelsUnlocked`). Đã bỏ chế độ chơi chung một máy.

Gói kim cương (mua một lần, tiêu thụ được): **1 USD = 5 kim cương**, **5 USD = 30 kim cương**.

## Việc phải làm trong Play Console (bắt buộc, nếu không cửa hàng sẽ hiện "Chưa khả dụng")

Tạo đúng các mã sản phẩm sau (phân biệt chữ thường), rồi **kích hoạt**:

| Loại | Mã sản phẩm | Giá |
|---|---|---|
| Đăng ký (subscription), 1 gói cơ sở lặp theo tháng | `pm_noads_monthly` | 1 USD / tháng |
| Đăng ký (subscription), 1 gói cơ sở lặp theo năm | `pm_pro_yearly` | 10 USD / năm |
| Sản phẩm trong ứng dụng (one-time) | `pm_diamonds_5` | 1 USD |
| Sản phẩm trong ứng dụng (one-time) | `pm_diamonds_30` | 5 USD |

Các bước khác:

1. Ứng dụng phải được tải lên ít nhất một kênh (nội bộ/thử nghiệm kín) để Play Billing trả về sản phẩm. Thêm tài khoản kiểm thử ở *Cấu hình > Kiểm thử giấy phép*.
2. Tạo đơn vị quảng cáo **App open** trong AdMob và đặt biến Actions `ADMOB_APP_OPEN` (cùng chỗ với `ADMOB_BANNER`, `ADMOB_APP_ID`). Nếu thiếu, workflow build release sẽ báo lỗi thay vì xuất bản dùng ID thử nghiệm.
3. Cập nhật **Data safety** và chính sách quyền riêng tư: app dùng Google Play Billing, và lưu bản sao ví (số kim cương, số lượt miễn phí đã dùng) trong Google Block Store để khôi phục khi cài lại (xem bên dưới).

## Cách app xử lý giao dịch (`data/Billing.kt`)

* Dùng Google Play Billing Library **8.0.0**.
* **Kim cương** chỉ được cộng *sau khi* `consumeAsync` thành công. Mỗi giao dịch chỉ tiêu thụ được một lần nên không thể cộng hai lần, kể cả khi app bị tắt giữa chừng: lần mở sau, `refresh()` tìm giao dịch chưa tiêu thụ và xử lý tiếp.
* **Gói đăng ký**: xác nhận (acknowledge) trong 3 ngày để Google không hoàn tiền tự động. Gói hiện tại được lưu vào ví cùng thời điểm xác nhận cuối; nếu máy offline quá 8 ngày thì tạm coi là gói miễn phí, và tự khôi phục khi có mạng.
* Giao dịch ở trạng thái *đang chờ* (PENDING) chưa được tính; khi được duyệt sẽ tới ở lần `refresh()` kế tiếp.
* `refresh()` chạy mỗi lần mở app (`MainActivity.onResume`) và khi bấm "Khôi phục giao dịch".

**Hạn chế cần biết:** không có máy chủ nên việc xác minh giao dịch chỉ dựa vào thư viện Google Play trên máy. Người dùng root/mod có thể giả mạo kết quả cục bộ. Muốn chống gian lận triệt để, hãy xác minh `purchaseToken` bằng Google Play Developer API ở máy chủ và để máy chủ cấp quyền lợi.

## Lỗi "gỡ app cài lại là có lượt miễn phí" - đã sửa như thế nào

Nguyên nhân: ví nằm trong thư mục riêng của app và khóa mã hóa nằm trong Android Keystore; cả hai bị xóa khi gỡ app, nên cài lại là ví trống.

Cách xử lý (ba lớp độc lập, lấy giá trị "đã dùng nhiều nhất" của cả ba):

1. **Google Block Store** (`BlockStoreBackup.kt`): bản sao ví có chữ ký, lưu ở Play services nên *không bị xóa khi gỡ app*. Cài lại sẽ tự khôi phục (màn hình chờ tối đa ~3 giây).
2. **Tệp đánh dấu `pm_marks.xml`**: chỉ chứa số lượt miễn phí đã dùng trong ngày, được Auto Backup của Android sao lưu (`backup_rules.xml`, `data_extraction_rules.xml`). Ví và khóa vẫn *không* được sao lưu.
3. **Bộ đếm cục bộ** như trước (chống chép lại tệp ví cũ).

**Giới hạn thật sự (không có máy chủ thì không thể tránh hoàn toàn):**
xóa dữ liệu ứng dụng trong Cài đặt + gỡ Play services/đăng xuất tài khoản Google, khôi phục cài đặt gốc, dùng thiết bị khác, hoặc máy đã root vẫn có thể đặt lại lượt miễn phí. Muốn chặn hẳn cần tài khoản đăng nhập hoặc một máy chủ nhỏ lưu theo Play Integrity token / tài khoản. **Cần kiểm tra trên máy thật** (gỡ và cài lại bản release từ Play) trước khi phát hành, vì Block Store phụ thuộc phiên bản Play services của từng máy.

## Cảnh báo Play Console đã xử lý

* *"App Bundle chứa mã gốc và chưa có biểu tượng gỡ lỗi"* → `ndk { debugSymbolLevel = "FULL" }` trong bản release, biểu tượng nằm ngay trong `.aab`. Nếu thư viện của bên thứ ba (WebRTC, AdMob) phát hành sẵn bản đã bị lược bỏ biểu tượng thì Play có thể vẫn nhắc nhẹ; đó là giới hạn của thư viện, không phải lỗi của app.
* *"Hạn chế phạm vi cung cấp do tính năng phần cứng bắt buộc `android.hardware.screen.portrait`"* → khai báo `android.hardware.screen.portrait` và `landscape` với `required="false"` trong `AndroidManifest.xml`. App vẫn khóa chiều dọc nhưng không còn loại các thiết bị (máy tính bảng, Chromebook, PC...) khỏi danh sách cung cấp.
