# Giấy phép bên thứ ba

Play Mind chỉ đóng gói mã và dữ liệu có giấy phép **cho phép dùng thương mại trong ứng dụng đóng**: MIT, BSD, Apache-2.0, CC0, MPL-2.0 (tuân thủ theo từng tệp), SIL OFL cho phông chữ. Mã GPL/AGPL/LGPL, "chỉ phi thương mại" hoặc "không ghi giấy phép" không được đóng gói. Game nào không có engine đạt điều kiện này thì bot được tự viết (BOT_GUIDE mục 4.0).

Ứng dụng hiển thị toàn bộ các thông báo này ở **Cài đặt → Giấy phép mã nguồn mở** (tệp `app/src/main/assets/third_party_notices.txt`, ghép từ các tệp trong thư mục này).

## Đang đóng gói trong ứng dụng

| Thành phần | Dùng cho | Phiên bản | Giấy phép | Tệp |
|---|---|---|---|---|
| KataGo | Bot cờ vây (engine nền, BOT_GUIDE mục 8) | v1.18.2 | MIT (mã) | [KataGo-LICENSE.txt](KataGo-LICENSE.txt) |
| Mạng g170-b6c96-s175395328-d26788732 | Cờ vây cấp Dễ, Trung bình, Khá | g170 | CC0 (từ bỏ bản quyền) | [KataGo-g170-networks-CC0.txt](KataGo-g170-networks-CC0.txt) |
| Mạng g170e-b10c128-s1141046784-d204142634 | Cờ vây cấp Giỏi đến Siêu việt | g170 | CC0 | [KataGo-g170-networks-CC0.txt](KataGo-g170-networks-CC0.txt) |
| ghc::filesystem, half, TCLAP, nlohmann/json, sha2.cpp | Biên dịch kèm trong KataGo | theo KataGo v1.18.2 | MIT / BSD-style | [KataGo-third-party.txt](KataGo-third-party.txt) |
| Eigen | Phép tính mạng nơ-ron trên CPU của KataGo (chỉ dùng header, không sửa đổi) | 3.4.0 | MPL-2.0 | [Eigen-MPL2.txt](Eigen-MPL2.txt) |
| Manrope, Unbounded | Phông chữ giao diện | | SIL OFL 1.1 | [OFL-Manrope.txt](OFL-Manrope.txt), [OFL-Unbounded.txt](OFL-Unbounded.txt) |

Ghi chú:

* **Eigen (MPL-2.0)** được dùng nguyên bản, không sửa tệp nào. MPL-2.0 chỉ yêu cầu công bố mã của các tệp thuộc MPL; mã nguồn đúng phiên bản có tại <https://gitlab.com/libeigen/eigen/-/tree/3.4.0>. Nếu sau này sửa tệp nào của Eigen thì phải công bố tệp đã sửa.
* **zlib** dùng bản có sẵn của Android (`libz.so` của hệ thống), không đóng gói.
* Thư viện C++ chuẩn của NDK (libc++, Apache-2.0 có ngoại lệ LLVM) được liên kết tĩnh; ngoại lệ LLVM miễn yêu cầu ghi chú khi phân phối dạng nhị phân.
* Tệp chạy KataGo và hai mạng được tải, kiểm mã băm SHA-256 và biên dịch trên CI bằng `tools/katago/build_android.sh`; không commit vào kho mã.

## Đã tra cứu nhưng KHÔNG đóng gói (bot tự viết)

Tra cứu ngày 27/11/2026 (chi tiết ở BOT_GUIDE mục 4.0):

| Game | Engine tìm thấy | Giấy phép | Lý do không dùng |
|---|---|---|---|
| Cờ vua | Stockfish | GPL-3.0 | Không cho phép đóng gói vào ứng dụng đóng. Các ứng viên MIT (Leorik, Zahak, Arasan, Caissa) chưa qua cổng kỹ thuật (build NDK, luật lặp thế riêng của app) nên chưa dùng; bot tự viết |
| Cờ tướng | Pikafish | GPL-3.0, mạng có điều khoản riêng | Không đạt cổng giấy phép; bot tự viết |
| Cờ caro | Rapfi và các engine Gomocup | GPL-3.0 / chơi luật khác luật chặn hai đầu của app | Không đạt cổng giấy phép hoặc cổng luật; bot tự viết |
| Quoridor | Dự án sở thích, bài tập | Không đồng nhất | Không có engine chuẩn; bot tự viết |
| Wall Go | Bản remake (MIT, AI yếu), WallZero (chưa rõ giấy phép) | | Không đủ mạnh / không rõ giấy phép; bot tự viết |

Các engine GPL ở trên chỉ được phép dùng làm **engine đo** trên máy phát triển (BOT_GUIDE mục 3.5), không bao giờ đóng gói.
