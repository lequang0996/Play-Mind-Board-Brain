# Thiết kế giao diện "Pearl & Champagne"

App chỉ có giao diện sáng: nền ngọc trai xanh nhạt, thẻ trắng nổi bằng bóng đổ mềm tông xanh mực (`Modifier.lift`), viền mảnh 1 dp, chữ xanh navy đậm. Vàng champagne chỉ dùng cho hành động chính và kim cương (cùng tông với biểu tượng app). Hai điểm nhấn tối sang trọng: két kim cương ở trang chủ và thẻ gói năm ở cửa hàng (bộ màu `deep`). Mỗi chủ đề có một màu nhận diện làm vầng sáng cho thẻ game; khi dùng làm chữ thì làm đậm bằng `Color.onLight()`.

## Mã màu (`ui/Theme.kt`, bảng `PmLight`)

| Vai trò | Giá trị |
|---|---|
| Nền / nền sâu | `#F4F5FA` / `#E7EAF4` |
| Mặt thẻ / mặt nổi | `#FFFFFF` / `#F6F7FC` |
| Viền | `#E2E5F0` |
| Chữ chính / chữ phụ | `#141A33` / `#5E6684` |
| Vàng chữ (đủ tương phản) / vàng mảng màu / sáng / đậm | `#8F6210` / `#D4A237` / `#F3DC95` / `#A97C1F` |
| Thẻ nhấn tối (deep, deepHigh, onDeep) | `#121733`, `#222A5A`, `#F6F1E4` |
| Thành công / cảnh báo | `#139A66` / `#D93C3C` |

## Phông chữ (đặt trong `res/font`)

* **Unbounded Bold** cho tiêu đề và số lớn: rộng, hình học, có chất "game". Dùng ở cỡ vừa, không giãn chữ.
* **Manrope** (Medium, SemiBold, Bold, ExtraBold) cho nội dung và nhãn.
* Cả hai đều hỗ trợ Latin, tiếng Việt đầy đủ dấu và Cyrillic; chữ Hán/Nhật/Hàn tự rơi về phông hệ thống. Bản đã cắt gọn (subset) còn ~600 KB cho cả bộ.
* Giấy phép SIL OFL 1.1: xem `docs/licenses/OFL-*.txt`. Nếu đổi phông, nhớ kiểm tra đủ ký tự tiếng Việt (ví dụ Sora và Outfit **thiếu** nhiều dấu tiếng Việt nên đã bị loại).

## Thành phần dùng chung (`ui/Components.kt`)

`PmDialog` (hộp thoại thương hiệu thay AlertDialog: huy hiệu tròn phát sáng, tiêu đề, nội dung, nút), `GoldButton`, `OutlineButton`, `SurfaceCard`, `Pill`, `OptionChips`, `DiamondChip`, `GemMark`, `PmIconButton`. Mọi popup trong app đi qua `SimpleDialog`/`PmDialog`.

## Quy ước

* Không dùng nhãn viết hoa toàn bộ; tiêu đề phụ viết thường, màu vàng.
* Số, thời gian, điểm dùng phông tiêu đề; mô tả dùng Manrope màu chữ phụ.
* Bàn cờ giữ chất liệu thật (gỗ, đá, sơn mài) trên nền tối để nổi bật.
