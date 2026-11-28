#!/usr/bin/env python3
"""Sinh thư mục docs/ (dùng cho GitHub Pages): index, hướng dẫn từng game, privacy-policy.html. EN + VI."""
import os, sys
root = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
docs = os.path.join(root, "docs")
os.makedirs(os.path.join(docs, "how-to-play"), exist_ok=True)
EMAIL = "17eathday@gmail.com"
DEV = "FWs"
APP = "Play Mind: Board & Brain"
DATE_EN = "November 20, 2026"
DATE_VI = "20/11/2026"

CSS = """
:root{--bg:#f5f3fa;--card:#fff;--ink:#16162f;--soft:#5c5c7a;--gold:#9c6d12;--line:#dad5e8;--chip:#ece8f5}
@media (prefers-color-scheme:dark){:root{--bg:#0a0d1d;--card:#141832;--ink:#f2effa;--soft:#a4a7cb;--gold:#e8c56e;--line:#2c3158;--chip:#1e2347}}
*{box-sizing:border-box}html{-webkit-text-size-adjust:100%}
body{margin:0;background:var(--bg);color:var(--ink);font:16px/1.65 -apple-system,"Segoe UI",Roboto,"Noto Sans",sans-serif}
.wrap{max-width:820px;margin:0 auto;padding:28px 20px 64px}
header{display:flex;align-items:center;gap:14px;flex-wrap:wrap;justify-content:space-between;margin-bottom:8px}
.brand{font-family:Georgia,"Noto Serif",serif;font-weight:700;font-size:28px;letter-spacing:-.4px}
.brand small{display:block;font:600 12px/1 sans-serif;letter-spacing:.14em;color:var(--gold);text-transform:uppercase;margin-top:6px}
.lang{display:inline-flex;background:var(--chip);border-radius:12px;padding:4px;gap:4px}
.lang button{border:0;background:transparent;color:var(--soft);font:600 14px sans-serif;padding:8px 14px;border-radius:9px;cursor:pointer}
.lang button[aria-pressed=true]{background:var(--card);color:var(--ink);box-shadow:inset 0 0 0 1px var(--gold)}
h1{font-family:Georgia,"Noto Serif",serif;font-size:34px;line-height:1.2;margin:28px 0 6px}
h2{font-family:Georgia,"Noto Serif",serif;font-size:22px;margin:34px 0 8px;padding-top:6px}
h3{font-size:16px;margin:20px 0 4px;color:var(--gold)}
p,li{color:var(--ink)}li{margin:4px 0}.muted{color:var(--soft)}
a{color:var(--gold)}
.card{background:var(--card);border:1px solid var(--line);border-radius:20px;padding:18px 20px;margin:14px 0}
.grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(220px,1fr));gap:14px}
.grid a{text-decoration:none;color:inherit}.grid .card{height:100%;margin:0}.grid b{display:block;font-family:Georgia,serif;font-size:19px}
.topics{display:flex;flex-wrap:wrap;gap:8px;margin:14px 0}.topics>span{padding:6px 14px;border-radius:99px;color:#fff;font:600 13px sans-serif}
table{border-collapse:collapse;width:100%;margin:10px 0}th,td{border:1px solid var(--line);padding:8px 10px;text-align:left;vertical-align:top;font-size:15px}th{background:var(--chip)}
footer{margin-top:46px;color:var(--soft);font-size:14px;border-top:1px solid var(--line);padding-top:16px}
[data-l]{display:none}body[data-lang=en] [data-l=en],body[data-lang=vi] [data-l=vi]{display:revert}
noscript style{display:none}
"""

JS = """
(function(){var b=document.body,s=null;try{s=localStorage.getItem('pm_lang')}catch(e){}
var l=s||((navigator.language||'en').toLowerCase().indexOf('vi')===0?'vi':'en');
function set(x){b.setAttribute('data-lang',x);document.documentElement.lang=x;
document.querySelectorAll('.lang button').forEach(function(e){e.setAttribute('aria-pressed',e.dataset.set===x)});
try{localStorage.setItem('pm_lang',x)}catch(e){}}
document.querySelectorAll('.lang button').forEach(function(e){e.onclick=function(){set(e.dataset.set)}});set(l)})();
"""

def page(title_en, title_vi, body, back=False, base=""):
    back_html = f'<p><a href="{base}index.html">&larr; <span data-l="en">Home</span><span data-l="vi">Trang chủ</span></a></p>' if back else ""
    return f"""<!doctype html>
<html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>{title_en} · {APP}</title><meta name="description" content="{APP} — {title_en}">
<style>{CSS}</style><noscript><style>[data-l]{{display:revert!important}}</style></noscript></head>
<body data-lang="en"><div class="wrap">
<header><div class="brand">Play Mind<small>Board &amp; Brain</small></div>
<div class="lang" role="group" aria-label="Language"><button data-set="en" aria-pressed="true">English</button><button data-set="vi" aria-pressed="false">Tiếng Việt</button></div></header>
{back_html}
{body}
<footer><span data-l="en">© {DEV}. Contact: <a href="mailto:{EMAIL}">{EMAIL}</a></span><span data-l="vi">© {DEV}. Liên hệ: <a href="mailto:{EMAIL}">{EMAIL}</a></span></footer>
</div><script>{JS}</script></body></html>
"""

def bi(en, vi, tag="div"):
    return f'<{tag} data-l="en">{en}</{tag}><{tag} data-l="vi">{vi}</{tag}>'

# ───────────────────────── Hướng dẫn từng game ─────────────────────────
GAMES = {
"chess": dict(
  en_name="Chess", vi_name="Cờ vua",
  en_intro="The classic strategy game for two players. White moves first. Your goal is to checkmate the enemy king: attack it so that it cannot escape.",
  vi_intro="Trò chơi chiến thuật kinh điển cho hai người. Bên Trắng đi trước. Mục tiêu là chiếu hết vua đối phương: tấn công vua sao cho nó không còn đường thoát.",
  en_body="""
<h2>Pieces and how they move</h2>
<table><tr><th>Piece</th><th>Movement</th></tr>
<tr><td>Pawn</td><td>Steps forward one square (two from its starting square). Captures one square diagonally forward.</td></tr>
<tr><td>Knight</td><td>Jumps in an L shape: two squares one way and one square sideways. It can jump over pieces.</td></tr>
<tr><td>Bishop</td><td>Slides any distance diagonally.</td></tr>
<tr><td>Rook</td><td>Slides any distance along a rank or file.</td></tr>
<tr><td>Queen</td><td>Slides any distance in any straight line.</td></tr>
<tr><td>King</td><td>Moves one square in any direction. It can never move into check.</td></tr></table>
<h2>Special rules</h2>
<ul><li><b>Check:</b> your king is under attack. You must remove the threat on your next move.</li>
<li><b>Castling:</b> the king moves two squares towards a rook and the rook jumps to the other side. Neither piece may have moved, the squares between must be empty, and the king may not castle out of, through or into check.</li>
<li><b>En passant:</b> if an enemy pawn advances two squares and lands beside your pawn, you may capture it as if it had moved one square, but only on the very next move.</li>
<li><b>Promotion:</b> a pawn reaching the last rank becomes a queen, rook, bishop or knight of your choice.</li></ul>
<h2>How a game ends</h2>
<ul><li><b>Checkmate:</b> the king is in check and there is no legal move to escape. That player loses.</li>
<li><b>Repetition (app rule):</b> a move that recreates a position that has already appeared (same side to move) loses for the player who made it. Perpetual shuffling back and forth is therefore not allowed.</li>
<li><b>Draw:</b> stalemate (no legal move but not in check), 50 moves without a pawn move or capture, or not enough pieces to checkmate.</li>
<li><b>Resign:</b> you may resign at any time with the flag button.</li></ul>
<h2>Using the app</h2>
<ul><li>Tap one of your pieces; its legal destinations are highlighted. Tap a highlighted square to move.</li>
<li>The last move is tinted in gold and a king in check glows red.</li>
<li>Against the computer you always play White and move first.</li></ul>
<h2>Tips</h2><ul><li>Control the centre, develop knights and bishops early, castle to protect your king, and avoid moving the same piece twice in the opening.</li></ul>""",
  vi_body="""
<h2>Các quân cờ và cách đi</h2>
<table><tr><th>Quân</th><th>Cách đi</th></tr>
<tr><td>Tốt</td><td>Đi thẳng một ô (hai ô từ vị trí ban đầu). Ăn quân chéo phía trước một ô.</td></tr>
<tr><td>Mã</td><td>Nhảy hình chữ L: hai ô theo một hướng rồi một ô sang bên. Có thể nhảy qua quân khác.</td></tr>
<tr><td>Tượng</td><td>Đi chéo bao nhiêu ô cũng được.</td></tr>
<tr><td>Xe</td><td>Đi thẳng theo hàng ngang hoặc cột dọc, bao nhiêu ô cũng được.</td></tr>
<tr><td>Hậu</td><td>Đi thẳng hoặc chéo theo mọi hướng, bao nhiêu ô cũng được.</td></tr>
<tr><td>Vua</td><td>Đi một ô theo mọi hướng. Không bao giờ được đi vào ô bị chiếu.</td></tr></table>
<h2>Luật đặc biệt</h2>
<ul><li><b>Chiếu:</b> vua của bạn đang bị tấn công. Bạn phải hóa giải ngay ở nước kế tiếp.</li>
<li><b>Nhập thành:</b> vua đi hai ô về phía một xe, xe nhảy sang bên kia vua. Cả hai chưa từng di chuyển, các ô ở giữa phải trống, và vua không được nhập thành khi đang bị chiếu, đi qua hoặc vào ô bị chiếu.</li>
<li><b>Bắt tốt qua đường:</b> nếu tốt đối phương đi hai ô và đứng cạnh tốt của bạn, bạn có thể ăn nó như thể nó chỉ đi một ô, nhưng chỉ ngay ở nước kế tiếp.</li>
<li><b>Phong cấp:</b> tốt đi tới hàng cuối sẽ biến thành hậu, xe, tượng hoặc mã tùy bạn chọn.</li></ul>
<h2>Kết thúc ván cờ</h2>
<ul><li><b>Chiếu hết:</b> vua bị chiếu và không còn nước đi hợp lệ để thoát. Bên đó thua.</li>
<li><b>Lặp thế (luật của app):</b> nước đi tạo lại một thế cờ đã từng xuất hiện (cùng bên tới lượt) thì bên vừa đi bị xử thua. Vì vậy không thể đi qua đi lại để kéo dài ván.</li>
<li><b>Hòa:</b> hòa cờ (hết nước đi nhưng không bị chiếu), 50 nước không đi tốt hoặc ăn quân, hoặc không đủ quân để chiếu hết.</li>
<li><b>Đầu hàng:</b> bạn có thể đầu hàng bất cứ lúc nào bằng nút lá cờ.</li></ul>
<h2>Dùng trong app</h2>
<ul><li>Chạm vào quân của bạn, các ô đi được sẽ sáng lên. Chạm ô sáng để đi.</li>
<li>Nước đi gần nhất được tô vàng, vua đang bị chiếu phát sáng màu đỏ.</li>
<li>Khi chơi với máy bạn luôn cầm quân Trắng và đi trước.</li></ul>
<h2>Mẹo</h2><ul><li>Chiếm trung tâm, đưa mã và tượng ra sớm, nhập thành để bảo vệ vua và tránh đi một quân hai lần trong khai cuộc.</li></ul>"""),
"xiangqi": dict(
  en_name="Xiangqi (Chinese Chess)", vi_name="Cờ tướng",
  en_intro="Xiangqi is the chess of China and Vietnam (Cờ tướng). Red moves first. Your goal is to checkmate the enemy general.",
  vi_intro="Cờ tướng là môn cờ phổ biến ở Việt Nam và Trung Quốc. Bên Đỏ đi trước. Mục tiêu là chiếu bí tướng đối phương.",
  en_body="""
<h2>The board</h2>
<p>Pieces stand on the intersections of a 9×10 grid. A river runs across the middle and each side has a 3×3 palace marked with diagonals.</p>
<h2>Pieces and how they move</h2>
<table><tr><th>Piece (Red / Black)</th><th>Movement</th></tr>
<tr><td>General 帥 / 將</td><td>One step along a line, always inside the palace. The two generals may never face each other on an open file.</td></tr>
<tr><td>Advisor 仕 / 士</td><td>One step diagonally, inside the palace.</td></tr>
<tr><td>Elephant 相 / 象</td><td>Two points diagonally. It cannot cross the river and is blocked if the point in the middle is occupied.</td></tr>
<tr><td>Horse 傌 / 馬</td><td>One point along a line, then one diagonally outward. It is blocked if the adjacent point in its first direction is occupied ("hobbling the horse").</td></tr>
<tr><td>Chariot 俥 / 車</td><td>Any distance along a line, like a rook.</td></tr>
<tr><td>Cannon 炮 / 砲</td><td>Moves like a chariot, but to capture it must jump over exactly one piece (the screen) of either colour.</td></tr>
<tr><td>Soldier 兵 / 卒</td><td>One step forward. After crossing the river it may also step sideways. It never moves backward.</td></tr></table>
<h2>How a game ends</h2>
<ul><li><b>Checkmate:</b> the general is attacked and cannot escape.</li>
<li><b>No legal move:</b> in Xiangqi a player with no legal move loses.</li>
<li><b>Repetition (app rule):</b> a move that recreates a position that has already appeared (same side to move) loses for the player who made it. This replaces the tournament rules about perpetual check and chasing.</li>
<li><b>Draw in this app:</b> 120 half-moves without a capture.</li></ul>
<h2>Using the app</h2>
<ul><li>Tap a piece to see where it can go, then tap a highlighted point. Captures are shown with a gold ring.</li>
<li>Red plays from the bottom. If you play Black the board turns so your pieces are at the bottom.</li></ul>
<h2>Tips</h2><ul><li>Develop chariots early, use cannons from the centre file, keep advisors and elephants to protect the general and be careful of horses that can be hobbled.</li></ul>""",
  vi_body="""
<h2>Bàn cờ</h2>
<p>Quân cờ đặt trên các giao điểm của lưới 9×10. Giữa bàn cờ là con sông, mỗi bên có một cung 3×3 với các đường chéo.</p>
<h2>Các quân cờ và cách đi</h2>
<table><tr><th>Quân (Đỏ / Đen)</th><th>Cách đi</th></tr>
<tr><td>Tướng 帥 / 將</td><td>Đi một bước theo đường thẳng, luôn trong cung. Hai tướng không bao giờ được đối mặt nhau trên một cột khi không có quân cản.</td></tr>
<tr><td>Sĩ 仕 / 士</td><td>Đi một bước chéo, trong cung.</td></tr>
<tr><td>Tượng 相 / 象</td><td>Đi chéo hai điểm. Không được qua sông và bị cản nếu điểm ở giữa có quân ("cản mắt tượng").</td></tr>
<tr><td>Mã 傌 / 馬</td><td>Đi một điểm theo đường thẳng rồi một điểm chéo ra ngoài. Bị cản nếu điểm kề theo hướng đi đầu tiên có quân ("cản chân mã").</td></tr>
<tr><td>Xe 俥 / 車</td><td>Đi thẳng bao nhiêu điểm cũng được theo hàng ngang hoặc cột dọc.</td></tr>
<tr><td>Pháo 炮 / 砲</td><td>Đi như xe, nhưng khi ăn quân phải nhảy qua đúng một quân (ngòi) của bất kỳ bên nào.</td></tr>
<tr><td>Tốt 兵 / 卒</td><td>Đi tới một bước. Sau khi qua sông được đi thêm ngang. Không bao giờ đi lùi.</td></tr></table>
<h2>Kết thúc ván cờ</h2>
<ul><li><b>Chiếu bí:</b> tướng bị tấn công và không thể thoát.</li>
<li><b>Hết nước đi:</b> trong cờ tướng, bên không còn nước đi hợp lệ là bên thua.</li>
<li><b>Lặp thế (luật của app):</b> nước đi tạo lại một thế cờ đã từng xuất hiện (cùng bên tới lượt) thì bên vừa đi bị xử thua. Luật này thay cho luật thi đấu về chiếu dài và đuổi dài.</li>
<li><b>Hòa trong app này:</b> 120 nửa nước đi không ăn quân.</li></ul>
<h2>Dùng trong app</h2>
<ul><li>Chạm vào quân để xem nơi nó đi được, rồi chạm vào điểm sáng. Nước ăn quân hiện bằng vòng vàng.</li>
<li>Bên Đỏ chơi từ phía dưới. Nếu bạn cầm quân Đen, bàn cờ sẽ xoay để quân của bạn ở phía dưới.</li></ul>
<h2>Mẹo</h2><ul><li>Đưa xe ra sớm, dùng pháo ở cột giữa, giữ sĩ và tượng để bảo vệ tướng, và đề phòng bị cản chân mã.</li></ul>"""),
"caro": dict(
  en_name="Gomoku (Caro)", vi_name="Cờ caro (Gomoku)",
  en_intro="Take turns placing marks on an endless wooden board with no edges. X (dark wood) moves first and O is a light wooden ring. The first player to make an unbroken line of five or more marks wins. Against the computer you always move first.",
  vi_intro="Hai người lần lượt đặt quân lên bàn cờ gỗ vô hạn, không có mép bàn. X (gỗ sẫm) đi trước, O là vòng gỗ sáng. Ai xếp được năm quân trở lên liên tiếp trước sẽ thắng. Khi chơi với máy, bạn luôn đi trước.",
  en_body="""
<h2>How to win</h2>
<ul><li>Make a line of <b>five or more</b> of your own marks in a row: horizontal, vertical or diagonal.</li>
<li><b>Blocked at both ends does not count.</b> If both ends of your line are closed by enemy marks ), the line is not a win. At least one end must be open.</li>
<li>The board never runs out of room, so there are no draws by a full board.</li></ul>
<h2>Using the app</h2>
<ul><li>Drag with one finger to move around the board and pinch to zoom; the round buttons zoom and re-centre. Tap an empty cell to place your mark. The last move is outlined in gold.</li>
<li>A winning line is drawn in gold.</li></ul>
<h2>Terms</h2>
<table><tr><th>Name</th><th>Meaning</th></tr>
<tr><td>Open three</td><td>Three in a row with both ends open. If you ignore it, it becomes an open four.</td></tr>
<tr><td>Four</td><td>Four in a row with one open end. It forces your opponent to block immediately.</td></tr>
<tr><td>Open four</td><td>Four in a row with both ends open. It cannot be stopped, so it wins.</td></tr>
<tr><td>Double threat</td><td>One move that creates two threats at once (for example a four and an open three).</td></tr></table>
<h2>Tips</h2><ul><li>Start near the centre, always check your opponent's open threes, and look for moves that build two lines at once. The computer plays tactically, so watch for its fours.</li></ul>""",
  vi_body="""
<h2>Cách thắng</h2>
<ul><li>Xếp <b>năm quân trở lên</b> của mình liên tiếp: theo hàng ngang, cột dọc hoặc đường chéo.</li>
<li><b>Bị chặn cả hai đầu thì không tính.</b> Nếu cả hai đầu của hàng bị quân đối phương chặn, hàng đó không thắng. Ít nhất một đầu phải còn trống.</li>
<li>Bàn cờ không bao giờ hết chỗ nên không có chuyện hòa do đầy bàn.</li></ul>
<h2>Dùng trong app</h2>
<ul><li>Kéo một ngón để di chuyển trên bàn cờ, chụm hai ngón để thu phóng; các nút tròn giúp phóng to, thu nhỏ và về giữa. Chạm vào ô trống để đặt quân. Nước vừa đi được viền vàng.</li>
<li>Hàng thắng được vẽ bằng vạch vàng.</li></ul>
<h2>Thuật ngữ</h2>
<table><tr><th>Tên</th><th>Ý nghĩa</th></tr>
<tr><td>Ba hở</td><td>Ba quân liên tiếp, hai đầu còn trống. Nếu bỏ qua, nó sẽ thành bốn hở.</td></tr>
<tr><td>Bốn</td><td>Bốn quân liên tiếp, một đầu còn trống. Buộc đối phương phải chặn ngay.</td></tr>
<tr><td>Bốn hở</td><td>Bốn quân liên tiếp, hai đầu còn trống. Không thể chặn hết nên sẽ thắng.</td></tr>
<tr><td>Đòn kép</td><td>Một nước tạo ra hai mối đe dọa cùng lúc (ví dụ một hàng bốn và một hàng ba hở).</td></tr></table>
<h2>Mẹo</h2><ul><li>Bắt đầu gần trung tâm, luôn kiểm tra các hàng ba hở của đối phương và tìm nước đi tạo hai hàng cùng lúc. Máy chơi rất thiên về chiến thuật nên hãy để ý các hàng bốn của nó.</li></ul>"""),
"go": dict(
  en_name="Go (19×19)", vi_name="Cờ vây (19×19)",
  en_intro="Two players take turns placing stones on the intersections of a 19×19 board. Black moves first. The goal is to control more of the board than your opponent. Against the computer you always play Black.",
  vi_intro="Hai người lần lượt đặt quân lên giao điểm của bàn 19×19. Đen đi trước. Mục tiêu là kiểm soát nhiều bàn cờ hơn đối thủ. Khi chơi với máy, bạn luôn cầm quân Đen.",
  en_body="""
<h2>Rules</h2>
<ul><li><b>Liberties and capture:</b> the empty points next to a group of connected stones are its liberties. A group with no liberties left is captured and removed.</li>
<li><b>No suicide:</b> you may not play a stone that leaves your own group without liberties, unless it captures enemy stones.</li>
<li><b>No repeated positions (positional superko):</b> a move may never recreate an earlier whole-board position. This includes the simple ko.</li>
<li><b>Passing:</b> you may pass instead of playing. Two passes in a row end the game.</li></ul>
<h2>Scoring</h2>
<ul><li>Area scoring (Tromp-Taylor style): each player scores their stones on the board plus the empty regions that touch only their stones.</li>
<li>White receives <b>7.5 points komi</b>, so there are no draws.</li>
<li><b>Dead stones are not removed automatically.</b> Capture hopeless enemy stones before you pass, or they still count for your opponent.</li></ul>
<h2>Using the app</h2>
<ul><li>Tap an intersection to preview a stone, then press Place stone. After your opponent passes, an End game button appears: press it to pass as well and score the game.</li>
<li>The computer is powered by KataGo (open source, see the licenses page in Settings).</li></ul>
<h2>Tips</h2><ul><li>Corners first, then sides, then the centre. Keep your groups connected and make two eyes to keep a group alive.</li></ul>""",
  vi_body="""
<h2>Luật</h2>
<ul><li><b>Khí và bắt quân:</b> các giao điểm trống kề một nhóm quân liền nhau là khí của nhóm. Nhóm hết khí bị bắt và nhấc khỏi bàn.</li>
<li><b>Cấm tự sát:</b> không được đặt quân khiến nhóm của mình hết khí, trừ khi nước đó bắt được quân đối phương.</li>
<li><b>Cấm lặp thế (siêu ko theo vị trí):</b> không được đi nước tạo lại toàn bộ thế cờ đã từng có. Luật ko thông thường nằm trong luật này.</li>
<li><b>Bỏ lượt:</b> có thể bỏ lượt thay vì đi. Hai lần bỏ lượt liên tiếp thì ván kết thúc.</li></ul>
<h2>Tính điểm</h2>
<ul><li>Tính theo diện tích (kiểu Tromp-Taylor): mỗi bên được điểm bằng số quân của mình trên bàn cộng các vùng trống chỉ tiếp giáp quân của mình.</li>
<li>Trắng được <b>7,5 điểm komi</b> nên không có hòa.</li>
<li><b>Quân chết không tự gỡ.</b> Hãy bắt hết quân đối phương đã chết trước khi bỏ lượt, nếu không chúng vẫn được tính cho đối phương.</li></ul>
<h2>Dùng trong app</h2>
<ul><li>Chạm một giao điểm để xem trước quân, rồi bấm Đặt quân. Khi đối thủ bỏ lượt, nút Kết thúc ván sẽ hiện ra: bấm để bỏ lượt theo và tính điểm.</li>
<li>Máy dùng KataGo (mã nguồn mở, xem trang giấy phép trong Cài đặt).</li></ul>
<h2>Mẹo</h2><ul><li>Chiếm góc trước, rồi tới biên, cuối cùng mới ra giữa. Giữ các nhóm quân liên kết và tạo hai mắt để nhóm sống.</li></ul>"""),
"quoridor": dict(
  en_name="Quoridor", vi_name="Quoridor",
  en_intro="Race your pawn to the opposite side of a 9×9 board while slowing your opponents down with walls. Playable with 2 or 4 players. The first pawn to reach its goal side wins.",
  vi_intro="Đưa quân của bạn tới cạnh đối diện của bàn 9×9, đồng thời dùng tường để làm chậm đối thủ. Chơi được 2 hoặc 4 người. Quân đầu tiên chạm cạnh đích sẽ thắng.",
  en_body="""
<h2>Setup</h2>
<ul><li><b>2 players:</b> pawns start in the middle of opposite edges; each player has <b>10 walls</b>.</li>
<li><b>4 players:</b> one pawn in the middle of each edge, turn order bottom → left → top → right; each player has <b>5 walls</b>. Your goal is the opposite edge.</li></ul>
<h2>On your turn, do one of these</h2>
<ul><li><b>Move your pawn</b> one square up, down, left or right (not through walls).</li>
<li><b>Jump:</b> if another pawn is directly next to you, jump straight over it. If there is a wall, an edge or another pawn behind it, step diagonally to either side of it instead.</li>
<li><b>Place a wall</b> two squares long between squares. Walls may not overlap or cross, and a wall may <b>never</b> cut off every path of any pawn to its goal.</li></ul>
<h2>How a game ends</h2>
<ul><li>The first pawn to reach its goal edge wins.</li>
<li><b>Repetition (app rule):</b> with 2 players, a pawn move that recreates an earlier position loses for the player who made it. With 4 players such a move is not allowed.</li>
<li>If nobody has won after 300 moves, the game is a draw.</li></ul>
<h2>Using the app</h2>
<ul><li>Tap a highlighted square to move. To place a wall, choose horizontal or vertical wall, tap near the gap you want to preview it (green = legal), then press Place wall.</li></ul>
<h2>Tips</h2><ul><li>Walls are worth most when your opponent is close to the goal. Do not spend them all early, and always keep your own path short.</li></ul>""",
  vi_body="""
<h2>Chuẩn bị</h2>
<ul><li><b>2 người:</b> hai quân đứng giữa hai cạnh đối diện; mỗi người có <b>10 tường</b>.</li>
<li><b>4 người:</b> mỗi cạnh một quân, thứ tự đi dưới → trái → trên → phải; mỗi người có <b>5 tường</b>. Đích là cạnh đối diện.</li></ul>
<h2>Mỗi lượt làm một trong các việc sau</h2>
<ul><li><b>Đi quân</b> một ô lên, xuống, trái hoặc phải (không xuyên tường).</li>
<li><b>Nhảy:</b> nếu có quân khác đứng sát cạnh, nhảy thẳng qua nó. Nếu phía sau quân đó là tường, mép bàn hoặc quân khác thì bước chéo sang một bên của nó.</li>
<li><b>Đặt tường</b> dài hai ô vào khe giữa các ô. Tường không được chồng hay cắt nhau và <b>không bao giờ</b> được chặn kín mọi đường tới đích của bất kỳ quân nào.</li></ul>
<h2>Kết thúc ván</h2>
<ul><li>Quân đầu tiên chạm cạnh đích thắng.</li>
<li><b>Lặp thế (luật của app):</b> khi chơi 2 người, đi quân tạo lại một thế đã từng có thì bên vừa đi bị xử thua. Khi chơi 4 người, nước đi như vậy không được phép.</li>
<li>Sau 300 nước mà chưa ai thắng thì ván hòa.</li></ul>
<h2>Dùng trong app</h2>
<ul><li>Chạm ô sáng để đi quân. Muốn đặt tường: chọn tường ngang hoặc dọc, chạm gần khe muốn đặt để xem trước (xanh = hợp lệ) rồi bấm Đặt tường.</li></ul>
<h2>Mẹo</h2><ul><li>Tường đáng giá nhất khi đối thủ đã gần đích. Đừng dùng hết sớm, và luôn giữ đường đi của mình ngắn.</li></ul>"""),
"wallgo": dict(
  en_name="Wall Go", vi_name="Wall Go",
  en_intro="A territory game on a 7×7 board for 2 to 4 players. Move your stones and build walls to fence off areas that only you occupy. The largest territory wins.",
  vi_intro="Trò chơi chiếm lãnh thổ trên bàn 7×7 cho 2 đến 4 người. Di chuyển quân và xây tường để rào lại những vùng chỉ có quân của bạn. Lãnh thổ lớn nhất thắng.",
  en_body="""
<h2>Placement phase</h2>
<ul><li>Players take turns placing stones on empty squares: <b>2 players</b> place 4 stones each in the order A-B-B-A-A-B-B-A; <b>3 players</b> place 3 stones each and <b>4 players</b> 2 stones each, in a "snake" order (1-2-3-4-4-3-2-1...).</li></ul>
<h2>Play phase</h2>
<ul><li>On your turn, pick one of your stones and move it <b>0, 1 or 2 squares</b> orthogonally. It may not pass through walls or other stones.</li>
<li>Then you <b>must</b> build a wall on one free side of the square where that stone now stands (the board edge already counts as a wall).</li>
<li>A player with no legal move is skipped.</li></ul>
<h2>How a game ends</h2>
<ul><li>The game ends when every area separated by walls contains stones of at most one player.</li>
<li>Your territory is the number of squares in the areas that contain only your stones. The most territory wins; a tie is broken by the largest single area; if still tied, it is a draw.</li>
<li>Walls only ever accumulate, so a position can never repeat.</li></ul>
<h2>Using the app</h2>
<ul><li>Tap one of your stones, tap a highlighted square (or the stone again to stay), then tap towards the side where you want the wall.</li></ul>
<h2>Tips</h2><ul><li>Close off a safe corner early, and use walls to cut your opponent's stones away from large open areas.</li></ul>""",
  vi_body="""
<h2>Giai đoạn đặt quân</h2>
<ul><li>Lần lượt đặt quân lên ô trống: <b>2 người</b> mỗi người 4 quân theo thứ tự A-B-B-A-A-B-B-A; <b>3 người</b> mỗi người 3 quân, <b>4 người</b> mỗi người 2 quân, theo thứ tự "rắn" (1-2-3-4-4-3-2-1...).</li></ul>
<h2>Giai đoạn chơi</h2>
<ul><li>Mỗi lượt chọn một quân của mình và đi <b>0, 1 hoặc 2 ô</b> theo hàng ngang hoặc dọc, không xuyên tường hay quân khác.</li>
<li>Sau đó <b>bắt buộc</b> xây một bức tường lên một cạnh còn trống của ô mà quân vừa đứng (mép bàn coi như tường sẵn).</li>
<li>Người không còn nước đi hợp lệ bị bỏ lượt.</li></ul>
<h2>Kết thúc ván</h2>
<ul><li>Ván kết thúc khi mọi vùng bị tường ngăn chỉ còn quân của tối đa một người.</li>
<li>Lãnh thổ của bạn là tổng số ô của các vùng chỉ có quân của bạn. Nhiều lãnh thổ nhất thắng; bằng nhau thì xét vùng đơn lớn nhất; vẫn bằng thì hòa.</li>
<li>Tường chỉ tăng lên nên thế cờ không bao giờ lặp lại.</li></ul>
<h2>Dùng trong app</h2>
<ul><li>Chạm một quân của bạn, chạm ô sáng (hoặc chạm lại quân để đứng yên), rồi chạm về phía cạnh muốn đặt tường.</li></ul>
<h2>Mẹo</h2><ul><li>Rào sớm một góc an toàn, và dùng tường để tách quân đối thủ khỏi các vùng rộng.</li></ul>"""),
"sudoku": dict(
  en_name="Sudoku", vi_name="Sudoku",
  en_intro="A single-player number puzzle on a 9×9 grid made of nine 3×3 boxes. There is no opponent and no time limit.",
  vi_intro="Trò chơi số một người trên lưới 9×9 gồm chín khối 3×3. Không có đối thủ và không giới hạn thời gian.",
  en_body="""
<h2>Rules</h2>
<ul><li>Fill every empty square so that each row, each column and each 3×3 box contains the digits 1 to 9 exactly once.</li>
<li>Each 3×3 box starts with <b>only one given number</b> (9 givens in total), so many solutions are possible: any grid that follows the rules wins.</li>
<li>A number is marked wrong (red) when it clashes with another number in its row, column or box, or when it makes the grid impossible to finish. Mistakes are counted but never end the game.</li></ul>
<h2>Using the app</h2>
<ul><li>Tap a square, then a number. The keypad shows how many of each digit are still missing.</li>
<li>Pencil mode writes small candidate notes; Erase clears a square; Undo takes back your last action; Hint fills one correct square.</li></ul>
<h2>Tips</h2><ul><li>With so few givens, start where a row, column and box overlap the most filled squares, and use notes to keep track of candidates.</li></ul>""",
  vi_body="""
<h2>Luật</h2>
<ul><li>Điền mọi ô trống sao cho mỗi hàng, mỗi cột và mỗi khối 3×3 chứa các số từ 1 đến 9, mỗi số đúng một lần.</li>
<li>Mỗi khối 3×3 chỉ gợi ý sẵn <b>đúng một số</b> (cả bàn 9 số), nên có nhiều lời giải: mọi cách điền đúng luật đều thắng.</li>
<li>Số bị đánh dấu sai (màu đỏ) khi trùng với số khác trong cùng hàng, cột hoặc khối, hoặc khi khiến bàn không thể hoàn thành. Lỗi được đếm nhưng không bao giờ làm thua ván.</li></ul>
<h2>Dùng trong app</h2>
<ul><li>Chạm một ô rồi chạm số. Bàn phím số cho biết mỗi số còn thiếu bao nhiêu lần.</li>
<li>Bút chì để ghi chú số có thể điền; Xóa để xóa ô; Hoàn tác để lùi thao tác vừa làm; Gợi ý để điền đúng một ô.</li></ul>
<h2>Mẹo</h2><ul><li>Vì gợi ý rất ít, hãy bắt đầu ở nơi hàng, cột và khối có nhiều ô đã điền nhất, và dùng ghi chú để theo dõi các số có thể điền.</li></ul>"""),
"tangram": dict(
  en_name="Tangram", vi_name="Xếp hình 7 mảnh",
  en_intro="A single-player shape puzzle with the 7 classic tangram pieces. Each round has 10 random shapes and 3 minutes per shape.",
  vi_intro="Trò chơi ghép hình một người với 7 mảnh tangram cổ điển. Mỗi lượt có 10 hình ngẫu nhiên, mỗi hình 3 phút.",
  en_body="""
<h2>Rules</h2>
<ul><li>Use all 7 pieces (2 large triangles, 1 medium triangle, 2 small triangles, 1 square, 1 parallelogram) to fill the grey shape exactly, with no overlaps.</li>
<li>Each round shows 10 shapes, generated at random and getting harder: from compact shapes to spiky ones. Every shape can be solved.</li>
<li>You have 3 minutes per shape. If time runs out on any shape you lose the round; finish all 10 to win. Any arrangement that fills the shape counts.</li></ul>
<h2>Using the app</h2>
<ul><li>Drag a piece from the tray onto the shape; tap to rotate it by 45°; long-press the parallelogram to flip it. A piece dropped near a corner snaps into place.</li></ul>
<h2>Tips</h2><ul><li>Place the two large triangles first: together they cover half of the shape.</li></ul>""",
  vi_body="""
<h2>Luật</h2>
<ul><li>Dùng đủ 7 mảnh (2 tam giác lớn, 1 tam giác vừa, 2 tam giác nhỏ, 1 hình vuông, 1 hình bình hành) ghép kín hình xám, không chồng mảnh.</li>
<li>Mỗi lượt có 10 hình, sinh ngẫu nhiên và khó dần: từ hình gọn tới hình nhiều mỏm. Hình nào cũng giải được.</li>
<li>Mỗi hình có 3 phút. Hết giờ ở bất kỳ hình nào là thua lượt; ghép xong cả 10 hình là thắng. Mọi cách ghép kín đúng hình đều được tính.</li></ul>
<h2>Dùng trong app</h2>
<ul><li>Kéo mảnh từ khay lên hình; chạm để xoay 45°; nhấn giữ hình bình hành để lật. Thả mảnh gần một góc thì mảnh tự hít vào.</li></ul>
<h2>Mẹo</h2><ul><li>Đặt hai tam giác lớn trước: chúng chiếm một nửa hình.</li></ul>"""),
"blindcaro": dict(
  en_name="Blind Gomoku", vi_name="Cờ caro mù",
  en_intro="A memory twist on Gomoku: both players share stones in four colours, and the board never shows whose stone is whose.",
  vi_intro="Biến thể trí nhớ của cờ caro: hai bên dùng chung quân bốn màu và bàn cờ không cho biết quân nào của ai.",
  en_body="""
<h2>Rules</h2>
<ul><li>Same as Gomoku: get 5 of your own stones in a row (horizontal, vertical or diagonal) on an endless board; a line blocked at both ends does not count.</li>
<li>Stones come in 4 colours: blue, red, purple and yellow. The first player is X and the second is O whatever colour they use.</li>
<li>Each turn the first player places a stone of <b>any colour</b>; the second player must answer with a stone of <b>the same colour</b>.</li>
<li>The board shows only colours. You must remember which stones are yours; X and O marks are revealed when the game ends.</li></ul>
<h2>Using the app</h2>
<ul><li>Pick a colour on the palette in the bottom-left corner (the second player's colour is chosen automatically), then tap the board. Drag to move, pinch or use the buttons to zoom.</li></ul>
<h2>Tips</h2><ul><li>As the first player, switching colours often makes your stones harder to track. As the second player, remember positions rather than colours.</li></ul>""",
  vi_body="""
<h2>Luật</h2>
<ul><li>Như cờ caro: xếp được 5 quân của mình liên tiếp (ngang, dọc hoặc chéo) trên bàn vô hạn; dãy bị chặn cả hai đầu thì không tính.</li>
<li>Quân có 4 màu: xanh, đỏ, tím, vàng. Người đi trước là X, người đi sau là O, dù dùng màu nào.</li>
<li>Mỗi lượt người đi trước đặt một quân <b>màu bất kỳ</b>; người đi sau phải đáp lại bằng quân <b>cùng màu</b>.</li>
<li>Bàn cờ chỉ hiện màu. Bạn phải tự nhớ quân nào của mình; hết ván mới hiện dấu X / O.</li></ul>
<h2>Dùng trong app</h2>
<ul><li>Chọn màu ở bảng màu góc dưới bên trái (người đi sau được chọn sẵn màu bắt buộc), rồi chạm bàn để đặt quân. Kéo để di chuyển, chụm hoặc dùng nút để thu phóng.</li></ul>
<h2>Mẹo</h2><ul><li>Người đi trước đổi màu thường xuyên sẽ khiến quân khó theo dõi hơn. Người đi sau hãy nhớ theo vị trí, đừng nhớ theo màu.</li></ul>"""),
"magic7": dict(
  en_name="Magic Square 7×7", vi_name="Ma phương 7×7",
  en_intro="A single-player number puzzle on a 7×7 board. One new puzzle every day; there is no opponent and no time limit.",
  vi_intro="Trò chơi số một người trên bàn 7×7. Mỗi ngày một đề mới; không có đối thủ và không giới hạn thời gian.",
  en_body="""
<h2>Rules</h2>
<ul><li>Place the numbers 1 to 49, each exactly once, so that every row, every column and both main diagonals add up to <b>175</b>.</li>
<li>The puzzle starts with <b>one given number</b> in one cell. It always has a solution, and any arrangement that follows the rules wins.</li>
<li>One puzzle per day, the same for every player. After you solve it, the next puzzle arrives the following day.</li></ul>
<h2>Using the app</h2>
<ul><li>Tap a cell, then a number on the 49-key pad. Picking a number already on the board moves it to the selected cell.</li>
<li>The right column shows each row's sum, the bottom row each column's sum and the top bar both diagonals: green when a line is full and equals 175, red when it goes over 175 or is full but wrong.</li>
<li>Hints: 10 per day with the monthly plan, 20 per day with the yearly plan. A hinted cell is locked.</li>
<li>Leaving midway keeps your progress; come back the same day to continue.</li></ul>
<h2>Tips</h2><ul><li>175 = 7 × 25, so 25 is the middle number. Pairs that add up to 50 (1 + 49, 2 + 48...) placed symmetrically around the centre keep lines balanced.</li></ul>""",
  vi_body="""
<h2>Luật</h2>
<ul><li>Điền các số từ 1 đến 49, mỗi số đúng một lần, sao cho tổng của mỗi hàng, mỗi cột và hai đường chéo chính đều bằng <b>175</b>.</li>
<li>Đề bắt đầu với <b>một số cho sẵn</b> ở một ô. Đề luôn có lời giải và mọi cách điền đúng luật đều thắng.</li>
<li>Mỗi ngày một đề, giống nhau cho mọi người chơi. Giải xong thì sang ngày hôm sau mới có đề mới.</li></ul>
<h2>Dùng trong app</h2>
<ul><li>Chạm một ô rồi chạm số trên bàn phím 49 số. Chọn một số đã có trên bàn thì số đó được chuyển sang ô đang chọn.</li>
<li>Cột bên phải là tổng từng hàng, hàng dưới là tổng từng cột, thanh trên là tổng hai đường chéo: màu xanh khi đủ ô và đúng 175, màu đỏ khi vượt 175 hoặc đủ ô mà sai.</li>
<li>Gợi ý: gói tháng 10 lần mỗi ngày, gói năm 20 lần mỗi ngày. Ô được gợi ý bị khóa.</li>
<li>Thoát ra giữa chừng vẫn giữ nguyên bàn; vào lại trong ngày để giải tiếp.</li></ul>
<h2>Mẹo</h2><ul><li>175 = 7 × 25 nên 25 là số ở giữa. Các cặp có tổng 50 (1 + 49, 2 + 48...) đặt đối xứng qua tâm bàn giúp các hàng cân bằng.</li></ul>"""),
}

LEVELS_EN = '<h2>Computer levels and move timer</h2><ul><li>Seven computer levels: Easy, Medium, Good, Strong, Master, Challenger and Supreme. Only Easy is open at first; beat a level to unlock the next one for this game.</li><li>Your time per move against the computer depends on the level: Easy 1 min, Medium 2 min, Good 3 min, Strong 5 min, Master 10 min, Challenger 30 min, Supreme 1 hour. Against people the host picks one of the same seven times (1, 2, 3, 5, 10, 30 minutes or 1 hour). If time runs out, the player to move loses. The yearly plan unlocks every level at once.</li></ul>'
LEVELS_VI = '<h2>Cấp độ máy và thời gian mỗi nước</h2><ul><li>Máy có 7 cấp: Dễ, Trung bình, Khá, Giỏi, Cao thủ, Thách đấu và Siêu việt. Lúc đầu chỉ mở cấp Dễ; thắng một cấp sẽ mở cấp kế tiếp của game này.</li><li>Thời gian mỗi nước khi chơi với máy tùy cấp: Dễ 1 phút, Trung bình 2 phút, Khá 3 phút, Giỏi 5 phút, Cao thủ 10 phút, Thách đấu 30 phút, Siêu việt 1 giờ. Chơi với người: chủ phòng chọn một trong 7 mức đó (1, 2, 3, 5, 10, 30 phút hoặc 1 giờ). Hết giờ mà chưa đi thì người đang đến lượt bị xử thua. Gói năm mở ngay mọi cấp.</li></ul>'

for gid, g in GAMES.items():
    solo = gid in ("sudoku", "magic7")
    body = (f'<h1><span data-l="en">{g["en_name"]}</span><span data-l="vi">{g["vi_name"]}</span></h1>'
            + bi(f'<p class="muted">{g["en_intro"]}</p>' + g["en_body"] + ("" if solo else LEVELS_EN),
                 f'<p class="muted">{g["vi_intro"]}</p>' + g["vi_body"] + ("" if solo else LEVELS_VI)))
    open(os.path.join(docs, "how-to-play", f"{gid}.html"), "w", encoding="utf-8").write(
        page(g["en_name"] + " — How to play", g["vi_name"] + " — Cách chơi", body, back=True, base="../"))

# ───────────────────────── Trang chủ ─────────────────────────
index_body = f"""
<h1>{APP}</h1>
{bi("<p class='muted'>A collection of board and brain games for Android: Gomoku, Wall Go, Quoridor, Chess, Xiangqi, Go, Sudoku, Magic Square 7×7, Tangram and Blind Gomoku. Every two-or-more-player game has seven computer levels, from Easy to Supreme.</p>",
    "<p class='muted'>Bộ sưu tập game bàn cờ và trí tuệ cho Android: Cờ caro, Wall Go, Quoridor, Cờ vua, Cờ tướng, Cờ vây, Sudoku, Ma phương 7×7, Xếp hình 7 mảnh và Cờ caro mù. Mọi game từ 2 người trở lên đều có 7 cấp độ máy, từ Dễ đến Siêu việt.</p>")}
<div class="topics"><span style="background:#14937a"><span data-l="en">Puzzle</span><span data-l="vi">Giải đố</span></span><span style="background:#5348c9"><span data-l="en">Intellect</span><span data-l="vi">Trí tuệ</span></span><span style="background:#c27a0a"><span data-l="en">Calculation</span><span data-l="vi">Tính toán</span></span><span style="background:#c93e6b"><span data-l="en">Memory</span><span data-l="vi">Ghi nhớ</span></span></div>
<h2><span data-l="en">How to play</span><span data-l="vi">Hướng dẫn chơi</span></h2>
<div class="grid">
<a href="how-to-play/chess.html"><div class="card"><b><span data-l="en">Chess</span><span data-l="vi">Cờ vua</span></b><span class="muted" data-l="en">Pieces, special moves and ending a game.</span><span class="muted" data-l="vi">Quân cờ, nước đi đặc biệt và cách kết thúc ván.</span></div></a>
<a href="how-to-play/xiangqi.html"><div class="card"><b><span data-l="en">Xiangqi</span><span data-l="vi">Cờ tướng</span></b><span class="muted" data-l="en">Palace, river, cannons and horses.</span><span class="muted" data-l="vi">Cung, sông, pháo và mã.</span></div></a>
<a href="how-to-play/caro.html"><div class="card"><b><span data-l="en">Gomoku (Caro)</span><span data-l="vi">Cờ caro</span></b><span class="muted" data-l="en">Five in a row — blocked ends don't count.</span><span class="muted" data-l="vi">Năm quân liên tiếp, bị chặn hai đầu thì không tính.</span></div></a>
<a href="how-to-play/go.html"><div class="card"><b><span data-l="en">Go (19×19)</span><span data-l="vi">Cờ vây</span></b><span class="muted" data-l="en">Liberties, captures and area scoring.</span><span class="muted" data-l="vi">Khí, bắt quân và tính điểm diện tích.</span></div></a>
<a href="how-to-play/quoridor.html"><div class="card"><b>Quoridor</b><span class="muted" data-l="en">Race to the far side and block with walls.</span><span class="muted" data-l="vi">Chạy về cạnh đối diện và chặn bằng tường.</span></div></a>
<a href="how-to-play/wallgo.html"><div class="card"><b>Wall Go</b><span class="muted" data-l="en">Move, build a wall, claim territory.</span><span class="muted" data-l="vi">Đi quân, xây tường, chiếm lãnh thổ.</span></div></a>
<a href="how-to-play/sudoku.html"><div class="card"><b>Sudoku</b><span class="muted" data-l="en">One given per box, any valid grid wins.</span><span class="muted" data-l="vi">Mỗi khối một gợi ý, điền đúng luật là thắng.</span></div></a>
<a href="how-to-play/tangram.html"><div class="card"><b><span data-l="en">Tangram</span><span data-l="vi">Xếp hình 7 mảnh</span></b><span class="muted" data-l="en">10 random shapes, 3 minutes each.</span><span class="muted" data-l="vi">10 hình ngẫu nhiên, mỗi hình 3 phút.</span></div></a>
<a href="how-to-play/blindcaro.html"><div class="card"><b><span data-l="en">Blind Gomoku</span><span data-l="vi">Cờ caro mù</span></b><span class="muted" data-l="en">Shared four-colour stones: remember which are yours.</span><span class="muted" data-l="vi">Quân bốn màu dùng chung: nhớ quân nào của mình.</span></div></a>
<a href="how-to-play/magic7.html"><div class="card"><b><span data-l="en">Magic Square 7×7</span><span data-l="vi">Ma phương 7×7</span></b><span class="muted" data-l="en">Rows, columns and diagonals all sum to 175. Daily puzzle.</span><span class="muted" data-l="vi">Hàng, cột, đường chéo đều có tổng 175. Mỗi ngày một đề.</span></div></a>
</div>
<h2><span data-l="en">Using the app</span><span data-l="vi">Cách dùng ứng dụng</span></h2>
{bi('''<div class="card"><ul>
<li><b>Daily diamonds:</b> check in every day for +1 diamond. Each game gives 1 free play per day, then costs 1 diamond. A win gives +1 diamond.</li>
<li><b>Playing with friends:</b> each game costs every player 1 diamond (free plays do not apply); the winner gets +1. Monthly and yearly plans pay no diamonds.</li>
<li><b>Win streaks:</b> 3 wins in a row in one game give +2 diamonds; 5 in a row give +3.</li>
<li><b>Loyalty bonus:</b> after a 30-day check-in streak, each next check-in gives +2 diamonds (monthly plan: +4; the yearly plan has no diamonds).</li>
<li><b>Computer levels:</b> seven levels per game, from Easy to Supreme. Only Easy is open at first; beating a level unlocks the next. Your time per move grows with the level, up to 1 hour at Supreme; against people the host picks one of the same seven times. The yearly plan unlocks every level and removes diamonds entirely.</li>
<li><b>Repetition:</b> in Chess, Xiangqi and 2-player Quoridor, a move that recreates an earlier position loses.</li>
<li><b>Play modes:</b> against the computer, with a friend nearby (Google Nearby Connections, no internet), or online (WebRTC) by exchanging a room code or QR.</li>
<li><b>Rooms:</b> if nobody joins your room, an opponent is added automatically. Opponents in rooms may be computer-controlled.</li>
<li>Diamonds are virtual, cannot be bought and have no cash value.</li></ul></div>''',
'''<div class="card"><ul>
<li><b>Kim cương mỗi ngày:</b> điểm danh mỗi ngày +1 kim cương. Mỗi game có 1 ván miễn phí/ngày, sau đó tốn 1 kim cương mỗi ván. Thắng được +1 kim cương.</li>
<li><b>Chơi với bạn bè:</b> mỗi ván mỗi người -1 kim cương (không dùng lượt miễn phí), người thắng +1. Gói tháng hoặc gói năm không mất kim cương.</li>
<li><b>Chuỗi thắng:</b> thắng liên tiếp 3 ván ở một game được +2 kim cương, 5 ván được +3.</li>
<li><b>Thưởng gắn bó:</b> điểm danh liên tục 30 ngày thì mỗi lần điểm danh tiếp theo +2 kim cương (gói tháng: +4; gói năm không dùng kim cương).</li>
<li><b>Cấp độ máy:</b> mỗi game có 7 cấp từ Dễ đến Siêu việt. Lúc đầu chỉ mở cấp Dễ; thắng một cấp sẽ mở cấp kế tiếp. Thời gian mỗi nước tăng theo cấp, tối đa 1 giờ ở cấp Siêu việt; chơi với người thì chủ phòng chọn một trong 7 mức đó. Gói năm mở mọi cấp và bỏ hẳn kim cương.</li>
<li><b>Lặp thế:</b> ở Cờ vua, Cờ tướng và Quoridor 2 người, nước đi tạo lại một thế cờ đã có sẽ bị xử thua.</li>
<li><b>Chế độ chơi:</b> với máy, với bạn ở gần (Google Nearby Connections, không cần internet) hoặc online (WebRTC) bằng cách trao đổi mã phòng/QR.</li>
<li><b>Phòng:</b> nếu không ai vào phòng, hệ thống tự thêm đối thủ. Đối thủ trong phòng có thể do máy điều khiển.</li>
<li>Kim cương là vật phẩm ảo, không thể mua và không có giá trị quy đổi thành tiền.</li></ul></div>''')}
<h2><span data-l="en">Legal</span><span data-l="vi">Pháp lý</span></h2>
<p><a href="privacy-policy.html"><span data-l="en">Privacy policy</span><span data-l="vi">Chính sách quyền riêng tư</span></a></p>
"""
open(os.path.join(docs, "index.html"), "w", encoding="utf-8").write(page("Home", "Trang chủ", index_body))

# ───────────────────────── Privacy policy ─────────────────────────
pp_en = f"""
<h1>Privacy Policy</h1>
<p class="muted">App: <b>{APP}</b> (Android) · Developer: <b>{DEV}</b> · Contact: <a href="mailto:{EMAIL}">{EMAIL}</a> · Last updated: {DATE_EN}</p>
<p>This policy explains what information {APP} (the "App") uses and how. We designed the App so that it works without an account and without any server operated by us. We do not sell personal information.</p>

<h2>1. Summary</h2>
<ul><li>No account, sign-up, e-mail or phone number is required.</li>
<li>We (the developer) do not operate a server and do not receive your game data.</li>
<li>The App shows ads served by <b>Google AdMob</b>, which may collect and use certain device information as described in section 4.</li>
<li>Multiplayer connects your device directly to your opponent's device (peer-to-peer).</li></ul>

<h2>2. Information stored on your device</h2>
<p>The App stores the following on your device only: your diamond balance, daily check-in and win-streak counters, per-game statistics, your chosen language and appearance, and a nickname you may set (a random one such as "Player1234" is used by default). The wallet data is encrypted with a key held in the Android Keystore and is excluded from cloud backup and device transfer. Uninstalling the App deletes this data. We cannot access it.</p>

<h2>3. Information used by multiplayer features</h2>
<ul><li><b>Nearby play (Google Nearby Connections):</b> uses Bluetooth and Wi-Fi to discover and connect to devices close to you. On Android 12 and above the App asks for the "Nearby devices" permissions (Bluetooth scan/advertise/connect, Nearby Wi-Fi devices). On Android 11 and below it asks for the location permission, which the Android system requires for Bluetooth discovery; the App does not read or store your location. During a game, your nickname and moves are exchanged directly with your opponent's device.</li>
<li><b>Internet play (WebRTC):</b> the App creates a peer-to-peer connection with your opponent. You and your opponent exchange a connection code (text or QR) yourselves; no server of ours is involved. To find a connection path, the App contacts Google's public STUN servers (stun.l.google.com), which means your IP address is visible to Google's STUN service and, as part of the connection code, to your opponent. Your nickname and moves are then sent directly to your opponent, encrypted in transit by WebRTC (DTLS).</li>
<li><b>QR scanning:</b> scanning a code uses Google's code scanner (ML Kit, delivered through Google Play services). The App itself does not request camera permission and does not store images.</li>
<li><b>Opponents in rooms:</b> when you create a room and no human joins, an opponent controlled by the App (a computer player) may be added. Computer opponents do not collect or send any data.</li></ul>

<h2>4. Advertising (Google AdMob)</h2>
<p>The App displays banner and app-open ads through <b>Google AdMob</b> (Google LLC). AdMob and Google may collect and use, for serving, measuring and personalising ads, analytics and fraud prevention: the Android advertising ID, IP address, device and operating-system information, app interaction and diagnostic data, and approximate location derived from IP address. AdMob may use identifiers and similar technologies on your device. The App declares the <code>AD_ID</code> permission for this purpose.</p>
<ul><li><b>Consent:</b> where required (for example in the European Economic Area, the United Kingdom and Switzerland) the App shows Google's consent form (User Messaging Platform) before requesting ads, and you can change your choice at any time in <i>Settings → Privacy options</i>.</li>
<li><b>Controls:</b> you can reset your advertising ID or opt out of ads personalisation in your Android settings (Settings → Google → Ads, or Settings → Privacy → Ads).</li>
<li><b>Ad formats:</b> the App shows only a banner ad on the home screen and an app-open ad when the App is opened or brought back to the foreground (at most once every few minutes). There are no interstitial or rewarded ads.</li>
<li>More information: <a href="https://policies.google.com/privacy">Google Privacy Policy</a> · <a href="https://policies.google.com/technologies/partner-sites">How Google uses data from sites or apps that use its services</a> · <a href="https://support.google.com/admob/answer/6128543">AdMob &amp; Firebase data disclosure</a>.</li></ul>

<h2>5. Google Play services</h2>
<p>The App uses Google Play services for in-app updates (Google Play delivers new versions in the background), Nearby Connections and the code scanner. These services are governed by Google's own privacy policy.</p>

<h2>6. Children</h2>
<p>The App is not directed to children under 13 (or the minimum age in your country) and we do not knowingly collect personal information from children. If you believe a child has provided information, contact us at {EMAIL}.</p>

<h2>7. Data security and retention</h2>
<p>Game data is kept on your device and protected by Android Keystore encryption. We keep no copy. Data used by AdMob and Google Play services is retained by Google under its own policies.</p>

<h2>8. Your choices and rights</h2>
<p>Depending on where you live (for example under the GDPR or CCPA) you may have the right to access, delete or object to the processing of your personal data. Because we do not hold your data, you can delete everything by uninstalling the App. For data held by Google (advertising), use your Google account controls or Android settings. You can also write to {EMAIL}.</p>

<h2>9. Changes</h2>
<p>We may update this policy when the App changes. The date at the top shows the latest version. Continued use after an update means you accept it.</p>

<h2>10. Contact</h2>
<p>{DEV} · <a href="mailto:{EMAIL}">{EMAIL}</a></p>
"""
pp_vi = f"""
<h1>Chính sách quyền riêng tư</h1>
<p class="muted">Ứng dụng: <b>{APP}</b> (Android) · Nhà phát triển: <b>{DEV}</b> · Liên hệ: <a href="mailto:{EMAIL}">{EMAIL}</a> · Cập nhật lần cuối: {DATE_VI}</p>
<p>Chính sách này giải thích {APP} ("Ứng dụng") sử dụng thông tin nào và như thế nào. Ứng dụng được thiết kế để chơi mà không cần tài khoản và không cần máy chủ do chúng tôi vận hành. Chúng tôi không bán thông tin cá nhân.</p>

<h2>1. Tóm tắt</h2>
<ul><li>Không cần tài khoản, đăng ký, e-mail hay số điện thoại.</li>
<li>Chúng tôi (nhà phát triển) không vận hành máy chủ và không nhận dữ liệu chơi game của bạn.</li>
<li>Ứng dụng hiển thị quảng cáo do <b>Google AdMob</b> cung cấp, AdMob có thể thu thập và sử dụng một số thông tin thiết bị như mô tả ở mục 4.</li>
<li>Chế độ nhiều người chơi kết nối trực tiếp thiết bị của bạn với thiết bị đối thủ (ngang hàng).</li></ul>

<h2>2. Thông tin lưu trên thiết bị của bạn</h2>
<p>Ứng dụng chỉ lưu trên thiết bị của bạn: số dư kim cương, bộ đếm điểm danh và chuỗi thắng, thống kê từng game, ngôn ngữ và giao diện bạn chọn, và biệt danh bạn đặt (mặc định là tên ngẫu nhiên như "Player1234"). Dữ liệu ví được mã hóa bằng khóa trong Android Keystore và bị loại khỏi sao lưu đám mây và chuyển thiết bị. Gỡ cài đặt Ứng dụng sẽ xóa dữ liệu này. Chúng tôi không thể truy cập dữ liệu đó.</p>

<h2>3. Thông tin dùng cho tính năng nhiều người chơi</h2>
<ul><li><b>Chơi gần nhau (Google Nearby Connections):</b> dùng Bluetooth và Wi-Fi để tìm và kết nối với thiết bị ở gần. Trên Android 12 trở lên, Ứng dụng xin quyền "Thiết bị ở gần" (quét/quảng bá/kết nối Bluetooth, thiết bị Wi-Fi ở gần). Trên Android 11 trở xuống, Ứng dụng xin quyền vị trí vì hệ điều hành Android yêu cầu cho việc dò tìm Bluetooth; Ứng dụng không đọc hay lưu vị trí của bạn. Trong ván đấu, biệt danh và nước đi được trao đổi trực tiếp với thiết bị đối thủ.</li>
<li><b>Chơi qua internet (WebRTC):</b> Ứng dụng tạo kết nối ngang hàng với đối thủ. Bạn và đối thủ tự trao đổi mã kết nối (văn bản hoặc QR); không có máy chủ của chúng tôi tham gia. Để tìm đường kết nối, Ứng dụng liên hệ máy chủ STUN công cộng của Google (stun.l.google.com), nghĩa là địa chỉ IP của bạn hiển thị với dịch vụ STUN của Google và, như một phần của mã kết nối, với đối thủ. Sau đó biệt danh và nước đi được gửi trực tiếp tới đối thủ, được WebRTC (DTLS) mã hóa khi truyền.</li>
<li><b>Quét QR:</b> việc quét mã dùng trình quét mã của Google (ML Kit, cung cấp qua Google Play services). Bản thân Ứng dụng không xin quyền camera và không lưu hình ảnh.</li>
<li><b>Đối thủ trong phòng:</b> khi bạn tạo phòng mà không có người thật vào, một đối thủ do Ứng dụng điều khiển (người chơi máy) có thể được thêm vào. Đối thủ máy không thu thập hay gửi bất kỳ dữ liệu nào.</li></ul>

<h2>4. Quảng cáo (Google AdMob)</h2>
<p>Ứng dụng hiển thị quảng cáo biểu ngữ, xen kẽ và có thưởng thông qua <b>Google AdMob</b> (Google LLC). AdMob và Google có thể thu thập và sử dụng, để phân phát, đo lường và cá nhân hóa quảng cáo, phân tích và chống gian lận: mã nhận dạng quảng cáo Android, địa chỉ IP, thông tin thiết bị và hệ điều hành, dữ liệu tương tác với ứng dụng và dữ liệu chẩn đoán, cùng vị trí gần đúng suy ra từ địa chỉ IP. AdMob có thể dùng mã định danh và công nghệ tương tự trên thiết bị của bạn. Ứng dụng khai báo quyền <code>AD_ID</code> cho mục đích này.</p>
<ul><li><b>Sự đồng ý:</b> khi pháp luật yêu cầu (ví dụ ở Khu vực Kinh tế châu Âu, Vương quốc Anh và Thụy Sĩ) Ứng dụng hiển thị biểu mẫu đồng ý của Google (User Messaging Platform) trước khi yêu cầu quảng cáo, và bạn có thể đổi lựa chọn bất cứ lúc nào tại <i>Cài đặt → Tùy chọn quyền riêng tư</i>.</li>
<li><b>Kiểm soát:</b> bạn có thể đặt lại mã nhận dạng quảng cáo hoặc tắt cá nhân hóa quảng cáo trong cài đặt Android (Cài đặt → Google → Quảng cáo, hoặc Cài đặt → Quyền riêng tư → Quảng cáo).</li>
<li><b>Tần suất:</b> quảng cáo xen kẽ chỉ hiện giữa các ván. Quảng cáo có thưởng chỉ hiện khi bạn chủ động chọn xem.</li>
<li>Thông tin thêm: <a href="https://policies.google.com/privacy?hl=vi">Chính sách quyền riêng tư của Google</a> · <a href="https://policies.google.com/technologies/partner-sites?hl=vi">Cách Google sử dụng dữ liệu từ trang web hoặc ứng dụng dùng dịch vụ của Google</a> · <a href="https://support.google.com/admob/answer/6128543">Thông tin về dữ liệu AdMob &amp; Firebase</a>.</li></ul>

<h2>5. Google Play services</h2>
<p>Ứng dụng dùng Google Play services cho cập nhật trong ứng dụng (Google Play tải phiên bản mới ngầm), Nearby Connections và trình quét mã. Các dịch vụ này tuân theo chính sách quyền riêng tư riêng của Google.</p>

<h2>6. Trẻ em</h2>
<p>Ứng dụng không hướng tới trẻ em dưới 13 tuổi (hoặc độ tuổi tối thiểu ở quốc gia của bạn) và chúng tôi không cố ý thu thập thông tin cá nhân của trẻ em. Nếu bạn cho rằng một trẻ em đã cung cấp thông tin, hãy liên hệ {EMAIL}.</p>

<h2>7. Bảo mật và thời gian lưu trữ</h2>
<p>Dữ liệu game được lưu trên thiết bị của bạn và bảo vệ bằng mã hóa Android Keystore. Chúng tôi không giữ bản sao nào. Dữ liệu do AdMob và Google Play services sử dụng được Google lưu theo chính sách của Google.</p>

<h2>8. Lựa chọn và quyền của bạn</h2>
<p>Tùy nơi bạn sống (ví dụ theo GDPR hoặc CCPA) bạn có thể có quyền truy cập, xóa hoặc phản đối việc xử lý dữ liệu cá nhân. Vì chúng tôi không giữ dữ liệu của bạn, bạn có thể xóa mọi thứ bằng cách gỡ cài đặt Ứng dụng. Với dữ liệu do Google giữ (quảng cáo), hãy dùng điều khiển trong tài khoản Google hoặc cài đặt Android. Bạn cũng có thể viết thư tới {EMAIL}.</p>

<h2>9. Thay đổi</h2>
<p>Chúng tôi có thể cập nhật chính sách này khi Ứng dụng thay đổi. Ngày ở đầu trang cho biết phiên bản mới nhất. Tiếp tục sử dụng sau khi cập nhật nghĩa là bạn chấp nhận.</p>

<h2>10. Liên hệ</h2>
<p>{DEV} · <a href="mailto:{EMAIL}">{EMAIL}</a></p>
"""
# docs/privacy-policy.html được sửa tay (có thêm Google Play Billing, Block Store) và MỚI HƠN mẫu pp_en/pp_vi ở trên:
# chỉ sinh lại khi truyền --privacy, sau khi đã cập nhật mẫu cho khớp, để không vô tình xóa các khai báo đó.
if "--privacy" in sys.argv:
    open(os.path.join(docs, "privacy-policy.html"), "w", encoding="utf-8").write(
        page("Privacy Policy", "Chính sách quyền riêng tư", bi(pp_en, pp_vi), back=True))
open(os.path.join(docs, ".nojekyll"), "w").write("")
print("docs ok")
