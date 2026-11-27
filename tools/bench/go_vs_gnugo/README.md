# Đo bot cờ vây với GNU Go

```bash
sudo apt-get install -y gnugo                       # cài vào /usr/games/gnugo
kotlinc core/src/main/kotlin tools/bench/go_vs_gnugo/GoGtp.kt -d gogtp.jar -jvm-target 17
# sửa đường dẫn JAR ở đầu match.py nếu cần, rồi: <cấp GNU Go> <ms/nước của bot> <số ván>
setsid nohup python3 tools/bench/go_vs_gnugo/match.py 1 3000 6 > gnugo.log 2>&1 &
```
Mỗi ván bot đổi màu luân phiên. Điểm được tính theo diện tích sau khi gỡ quân chết theo `final_status_list dead` của GNU Go (đừng dùng `final_score`, nó chỉ là ước lượng). Chạy một máy 1 CPU thì chạy tuần tự. Xem `docs/BOT_GUIDE.md` mục 11.
