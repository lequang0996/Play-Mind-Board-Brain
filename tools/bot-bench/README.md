# Bộ đo độ mạnh bot

Các chương trình `main` dùng để đo bot bằng trận tự đấu. Không nằm trong build của app; chạy bằng `kotlinc`.

## Chuẩn bị bản "cũ" để đấu A/B
```bash
cd core/src/main/kotlin/com/fws/playmind/core
sed 's/ChessGame/ChessOld/g; s/override val id = "chess"/override val id = "chess_old"/' Chess.kt > /tmp/ChessOld.kt     # chạy TRƯỚC khi sửa Chess.kt
```
Làm tương tự cho `XiangqiGame -> XiangqiOld`, `GomokuGame -> GomokuOld`.

## Chạy
```bash
kotlinc core/src/main/kotlin/com/fws/playmind/core/Game.kt core/src/main/kotlin/com/fws/playmind/core/Chess.kt /tmp/ChessOld.kt \
        tools/bot-bench/ChessMatch.kt -include-runtime -d /tmp/match.jar
java -jar /tmp/match.jar ab 10 250 1            # mới vs cũ: 10 ván, 250 ms/nước, seed 1
sudo apt-get install stockfish
java -jar /tmp/match.jar sf 6 500 1 2200        # đấu Stockfish giới hạn 2200 Elo
```
* `XiangqiVsPikafish.kt`: tải Pikafish từ GitHub Releases (cần file `.nnue` cùng thư mục), sửa đường dẫn trong `PF`.
* `XiangqiAB.kt`, `GomokuAB.kt`: A/B mới vs cũ cho cờ tướng và cờ caro.
* Chạy nền: `(setsid nohup java -jar x.jar ... > log 2>&1 < /dev/null &)`. Một CPU thì chạy tuần tự.

Xem `docs/BOT_GUIDE.md` để biết cách đọc kết quả.
