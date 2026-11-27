# Thiết kế giao diện "Midnight Lacquer"

App chỉ có giao diện tối: nền xanh mực gần đen như mặt sơn mài, các mặt phẳng nổi bằng sắc xanh tím nhạt, viền mảnh 1 dp, màu vàng đồng chỉ dùng cho hành động chính và kim cương (cùng tông với biểu tượng app). Mỗi chủ đề có một màu nhận diện làm vầng sáng cho thẻ game.

## Mã màu (`ui/Theme.kt`, bảng `PmDark`)

| Vai trò | Giá trị |
|---|---|
| Nền / nền sâu | `#0B0E1A` / `#070914` |
| Mặt thẻ / mặt nổi | `#141935` / `#1C2347` |
| Viền | `#2A3157` |
| Chữ chính / chữ phụ | `#F3EFE6` / `#A3A8C4` |
| Vàng (nút, kim cương) | `#E2B857`, sáng `#F7E3A1`, đậm `#A9812A` |
| Thành công / cảnh báo | `#3DDC97` / `#FF6B6B` |
| Chủ đề: Trí tuệ, Tính toán, Giải đố, Ghi nhớ | `#8B7BFF`, `#3FB6E8`, `#2DD4A0`, `#FF6B8B` |
| Bốn bên của Wall Go / Quoridor | đỏ `#E5483D`, xanh dương `#3C7DF0`, xanh lá `#2FB36A`, vàng `#F2C230` |

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
