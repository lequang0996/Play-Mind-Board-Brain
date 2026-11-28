#!/usr/bin/env python3
"""Sinh README.md (mô tả đăng Google Play, EN + VI) và kiểm tra giới hạn của Play:
tiêu đề 30, mô tả ngắn 80, mô tả đầy đủ 4000 ký tự."""
import os, sys
root = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")

EN = dict(
 title="Play Mind: Board & Brain",
 short="Chess, Xiangqi and Gomoku vs the computer, friends nearby or online.",
 full="""Play Mind: Board & Brain is a polished collection of classic board games and brain games, built around four ways to exercise your mind: Puzzle, Intellect, Calculation and Memory.

AVAILABLE NOW — INTELLECT
• Chess: full rules including castling, en passant, promotion, check, checkmate, stalemate and draws.
• Xiangqi (Chinese Chess): generals, advisors, elephants, horses, chariots, cannons and soldiers on the river board, including the flying-general rule.
• Gomoku (Caro): make five in a row on an endless board - drag to explore, pinch to zoom. A line blocked at both ends does not count.
More games for the Puzzle, Calculation and Memory topics are coming soon.

PLAY YOUR WAY
• Against the computer: 7 levels from Easy to Supreme. Start at Easy and win to unlock the next level; Go is powered by KataGo.
• With a friend nearby: no internet needed, powered by Google Nearby Connections.
• Over the internet: peer-to-peer WebRTC. Share a room code or QR code and you are connected. No account, no sign-up.
• Rooms: create a room and wait for a friend. If nobody joins, an opponent is added automatically (room opponents may be computer-controlled). You can remove an opponent at any time.

DAILY DIAMONDS
• Check in every day for +1 diamond.
• Every game gives you one free play per day. After that, each game costs 1 diamond.
• Win a game for +1 diamond. Win 3 in a row in one game for +2 more, and 5 in a row for +3 more.
• Keep a check-in streak above 7 days with more than 10 diamonds and your daily check-in is worth +2.
• Diamonds are virtual, cannot be bought and have no cash value.

DESIGNED WITH CARE
• A luxurious ivory-and-gold look, with hand-drawn boards and pieces and a refined colour for each topic.
• 8 languages: English, 简体中文, Español, Português, Русский, 日本語, 한국어 and Tiếng Việt. Choose yours on first launch and change it any time in Settings.
• Clear in-app guides for the app and for every game.
• Your progress is stored on your device and protected with encryption. No account required.
• In-app updates: new versions download quietly while you play.

ADS AND PRIVACY
The app contains ads shown by Google AdMob. Only a banner on the home screen and an app-open ad are shown; there are no interstitial or rewarded ads. Where required, you can manage your ad consent at any time in Settings. The privacy policy is available in the app and on this store page.

Questions or ideas? Write to 17eathday@gmail.com. We add new games over time. Thank you for playing!
FWs""",
)
VI = dict(
 title="Play Mind: Board & Brain",
 short="Cờ vua, cờ tướng, cờ caro: chơi với máy, bạn bè ở gần hoặc online.",
 full="""Play Mind: Board & Brain là bộ sưu tập game bàn cờ và trí tuệ được chăm chút kỹ lưỡng, xây dựng quanh bốn cách rèn luyện trí óc: Giải đố, Trí tuệ, Tính toán và Ghi nhớ.

ĐÃ CÓ — CHỦ ĐỀ TRÍ TUỆ
• Cờ vua: đầy đủ luật gồm nhập thành, bắt tốt qua đường, phong cấp, chiếu, chiếu hết, hòa cờ và các thế hòa.
• Cờ tướng: tướng, sĩ, tượng, mã, xe, pháo và tốt trên bàn cờ có sông, kể cả luật hai tướng không được đối mặt.
• Cờ caro (Gomoku): xếp năm quân liên tiếp trên bàn cờ vô hạn - kéo để di chuyển, chụm để thu phóng. Hàng bị chặn cả hai đầu thì không tính.
Các game thuộc chủ đề Giải đố, Tính toán và Ghi nhớ sẽ sớm ra mắt.

CHƠI THEO CÁCH CỦA BẠN
• Với máy: 7 cấp từ Dễ đến Siêu việt. Bắt đầu ở cấp Dễ, thắng để mở khóa cấp kế tiếp; Cờ vây dùng KataGo.
• Với bạn bè ở gần: không cần internet, dùng Google Nearby Connections.
• Qua internet: kết nối ngang hàng WebRTC. Chỉ cần chia sẻ mã phòng hoặc mã QR là kết nối được. Không cần tài khoản, không cần đăng ký.
• Phòng: tạo phòng và chờ bạn bè. Nếu không ai vào, hệ thống tự thêm đối thủ (đối thủ trong phòng có thể do máy điều khiển). Bạn có thể mời đối thủ ra khỏi phòng bất cứ lúc nào.

KIM CƯƠNG MỖI NGÀY
• Điểm danh mỗi ngày để nhận +1 kim cương.
• Mỗi game có một ván chơi miễn phí mỗi ngày. Sau đó mỗi ván tốn 1 kim cương.
• Thắng một ván được +1 kim cương. Thắng liên tiếp 3 ván ở một game được thêm +2, thắng liên tiếp 5 ván được thêm +3.
• Duy trì điểm danh liên tục trên 7 ngày và có hơn 10 kim cương thì mỗi lần điểm danh được +2.
• Kim cương là vật phẩm ảo, không thể mua và không có giá trị quy đổi thành tiền.

THIẾT KẾ TINH TẾ
• Giao diện sáng và tối sang trọng, mỗi chủ đề một màu sắc riêng.
• 8 ngôn ngữ: English, 简体中文, Español, Português, Русский, 日本語, 한국어 và Tiếng Việt. Chọn ngôn ngữ ở lần mở đầu tiên và đổi bất cứ lúc nào trong Cài đặt.
• Hướng dẫn rõ ràng trong ứng dụng cho cả app và từng game.
• Tiến trình được lưu trên thiết bị và bảo vệ bằng mã hóa. Không cần tài khoản.
• Cập nhật trong ứng dụng: phiên bản mới được tải ngầm khi bạn đang chơi.

QUẢNG CÁO VÀ QUYỀN RIÊNG TƯ
Ứng dụng có quảng cáo do Google AdMob hiển thị. Quảng cáo xen kẽ chỉ hiện giữa các ván, và bạn có thể nhận một kim cương bằng cách chủ động xem quảng cáo có thưởng. Khi pháp luật yêu cầu, bạn có thể quản lý sự đồng ý quảng cáo bất cứ lúc nào trong Cài đặt. Chính sách quyền riêng tư có trong ứng dụng và trên trang cửa hàng này.

Có thắc mắc hoặc ý tưởng? Hãy viết cho 17eathday@gmail.com. Chúng tôi sẽ bổ sung game mới theo thời gian. Cảm ơn bạn đã chơi!
FWs""",
)
LIMITS = dict(title=30, short=80, full=4000)
bad = False
for name, d in (("EN", EN), ("VI", VI)):
    for k, lim in LIMITS.items():
        n = len(d[k])
        print(f"{name} {k}: {n}/{lim}")
        if n > lim: bad = True; print("  !!! VƯỢT GIỚI HẠN")
if bad: sys.exit(1)

def block(d, lang):
    t = "Tiêu đề" if lang == "vi" else "Title"
    sd = "Mô tả ngắn" if lang == "vi" else "Short description"
    fd = "Mô tả đầy đủ" if lang == "vi" else "Full description"
    u = "ký tự" if lang == "vi" else "characters"
    return (f"**{t}** ({len(d['title'])}/30 {u})\n\n```\n{d['title']}\n```\n\n"
            f"**{sd}** ({len(d['short'])}/80 {u})\n\n```\n{d['short']}\n```\n\n"
            f"**{fd}** ({len(d['full'])}/4000 {u})\n\n```\n{d['full']}\n```\n")

readme = f"""# Play Mind: Board & Brain

Mô tả đăng lên Google Play · Google Play store listing text — `com.fws.playmind` · FWs · 17eathday@gmail.com

> Dán nội dung trong từng khung vào Play Console → *Main store listing*. Số ký tự đã được kiểm tra bằng `tools/build_readme.py` (tiêu đề ≤ 30, mô tả ngắn ≤ 80, mô tả đầy đủ ≤ 4.000).
> Paste each block into Play Console → *Main store listing*. Character counts are verified by `tools/build_readme.py` (title ≤ 30, short ≤ 80, full ≤ 4,000).

* Tài liệu cho nhà phát triển (build, ký, AdMob, GitHub Pages, đăng Play): [`docs/DEVELOPER.md`](docs/DEVELOPER.md)
* Hướng dẫn chơi & chính sách quyền riêng tư: thư mục [`docs/`](docs/)
* Ảnh, icon, feature graphic cho cửa hàng: thư mục [`store/`](store/)

---

## 🇬🇧 English (en-US)

{block(EN, "en")}
---

## 🇻🇳 Tiếng Việt (vi)

{block(VI, "vi")}
"""
open(os.path.join(root, "README.md"), "w", encoding="utf-8").write(readme)
print("README ok")
