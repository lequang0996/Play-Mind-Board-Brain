# Hướng dẫn cho nhà phát triển / Developer guide

Ứng dụng: **Play Mind: Board & Brain** · `applicationId = com.fws.playmind` · targetSdk 36 · minSdk 24
Kotlin + Jetpack Compose · module `:core` (luật cờ + AI, Kotlin thuần, có kiểm thử) và `:app`.

## 1. Đẩy lên GitHub và tự động build

```bash
git init && git add . && git commit -m "Initial commit"
git branch -M main
git remote add origin https://github.com/<tên-bạn>/PlayMind.git
git push -u origin main
```

Mỗi lần push lên `main` (và mỗi pull request), workflow **Build** (`.github/workflows/build.yml`) sẽ:
1. Chạy `gradle :core:selfCheck` (perft cờ vua/cờ tướng, chiếu hết, hòa, caro...).
2. Build `assembleDebug`.
3. Build `bundleRelease` + `assembleRelease` với **R8 bật** (`isMinifyEnabled` + `isShrinkResources`).
4. Tải Artifacts (APK debug, AAB/APK release, `mapping.txt`, native debug symbols) với **`retention-days: 1`**, nghĩa là **Artifacts tự động bị xóa sau 1 ngày**.

> Muốn áp dụng 1 ngày cho cả repo: *Settings → Actions → General → Artifact and log retention → 1 day*.
> Dự án không kèm `gradle-wrapper.jar`; CI dùng `gradle/actions/setup-gradle` với Gradle 8.14.3. Muốn build cục bộ: cài Gradle rồi chạy `gradle wrapper`.

### Ký bản release (bắt buộc để upload lên Play)
```bash
keytool -genkeypair -v -keystore release.jks -alias playmind -keyalg RSA -keysize 4096 -validity 10000
base64 -w0 release.jks     # macOS: base64 -i release.jks
```
Thêm vào *Settings → Secrets and variables → Actions → Secrets*: `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`. **Không commit file .jks.** Nên bật *Play App Signing* khi tạo app trên Play Console.

### ID AdMob thật
Mặc định dùng **ID thử nghiệm của Google** (an toàn khi phát triển). Trước khi phát hành, thêm `ADMOB_APP_ID` (dạng `ca-app-pub-xxx~yyy`), `ADMOB_BANNER` và `ADMOB_APP_OPEN` (dạng `ca-app-pub-xxx/yyy`) vào tab **Variables** (hoặc **Secrets**). App chỉ dùng banner + quảng cáo mở app. Workflow `Build` sẽ **dừng** nếu thiếu ID thật hoặc còn ID thử nghiệm, nên bản release không bao giờ hiện "Test Ad". Không bấm vào quảng cáo thật của chính mình.

## 2. GitHub Pages (hosting privacy policy)
*Settings → Pages → Build and deployment → Deploy from a branch → `main` / `/docs`*.
Địa chỉ sẽ là `https://<tên-bạn>.github.io/PlayMind/privacy-policy.html`. Dán URL này vào Play Console (*App content → Privacy policy*) và đặt biến `PRIVACY_URL` (Variables) để nút "Chính sách quyền riêng tư" trong app mở đúng trang (mặc định trong `app/build.gradle.kts` là `https://17eathday.github.io/PlayMind/...`, hãy sửa nếu username/repo khác).

## 3. Ảnh cho cửa hàng (`store/`)
* `icon-512.png`, `feature-graphic-1024x500.png`: sinh bằng `python3 tools/make_assets.py`.
* `screenshots/`: chạy workflow **Store screenshots** (*Actions → Store screenshots → Run workflow*). Workflow mở `ScreenshotActivity` (chỉ có trong bản debug) trên emulator, chụp màn hình thật của app (sáng/tối, English/Tiếng Việt) rồi commit vào `store/screenshots/`.

## 4. Checklist đăng Google Play
- [ ] Tạo app, bật Play App Signing, upload file `.aab` ký bằng keystore ở trên.
- [ ] **Store listing**: dán mô tả từ `README.md` (EN + VI), upload `store/`.
- [ ] **App content**: Privacy policy URL · Ads = *Yes, contains ads* · Data safety (xem dưới) · Content rating (IARC) · Target audience **13+** (không nhắm tới trẻ em) · Advertising ID = *Yes* (AdMob).
- [ ] **Data safety gợi ý**: Device or other IDs (Advertising ID) và dữ liệu chẩn đoán/tương tác ứng dụng: *collected & shared với Google AdMob* cho *Advertising*. Không thu thập tên, e-mail, vị trí chính xác. Dữ liệu được mã hóa khi truyền (WebRTC DTLS); người dùng không có tài khoản nên *không có yêu cầu xóa dữ liệu* (dữ liệu chỉ nằm trên máy).
- [ ] Mô tả đã nêu rõ có quảng cáo và đối thủ trong phòng có thể do máy điều khiển.
- [ ] Kiểm thử In-App Update: dùng *Internal app sharing* hoặc một track kiểm thử, cài bản versionCode thấp hơn rồi đẩy bản cao hơn lên track đó.
- [ ] Tải `mapping.txt` (Play Console → App bundle explorer → Downloads) để đọc stack trace đã làm rối.

## 4b. Thêm game mới
> Muốn bot của game mới đạt mức cao thủ: làm theo [`BOT_GUIDE.md`](BOT_GUIDE.md). Gặp lỗi build: xem [`TROUBLESHOOTING.md`](TROUBLESHOOTING.md).

1. Tạo luật + AI trong `core/` (implement `BoardGame`) và thêm kiểm thử vào `SelfCheck.kt`.
2. Thêm một dòng `GameInfo(...)` vào `Catalog.games` (chọn `Topic`) và các chuỗi `game_*`, `guide_*` trong `tools/i18n/*.txt`, rồi chạy `python3 tools/i18n/generate.py`.
3. Thêm bàn cờ vào `ui/Boards.kt` hoặc `ui/NewBoards.kt` và nhánh `when (info.id)` trong `GameScreen`; thêm tranh minh hoạ trong `GameArt` (`ui/Components.kt`, `ui/NewArt.kt`).
4. Game có tùy chọn trước ván: đặt `optionKind` (`PLAYERS` = số người chơi, `BOARD` = cỡ bàn) và `options` trong `GameInfo`; hàm `create` nhận tùy chọn đó. `localPlay = true` bật thêm chế độ chơi chung một máy.
5. Game một người (Sudoku, Ma phương 7×7): `kind = GameKind.SOLO`, trạng thái nằm trong `SessionViewModel` (`startSudoku`/`sudokuEdit`, `startMagic`/`magicEdit`/`magicHint`, vào chung qua `startSolo`) và có màn hình riêng (`SudokuScreen`, `MagicScreen`). Ma phương: luật và sinh đề ở core `MagicSquare.kt` (mỗi ngày một đề sinh từ số ngày, dùng họ ma phương toàn chéo nên luôn có lời giải; gợi ý tìm lời giải trong họ khớp nhiều ô người chơi đã điền nhất); ván đang giải dở lưu ở `Prefs.magicSave`; trả lượt / kim cương một lần mỗi ngày (`startMatch` rồi `cancelMatch` để thoát giữa chừng không tính thua); gợi ý mỗi ngày theo gói: `Catalog.MAGIC_HINTS_MONTHLY` = 10, `MAGIC_HINTS_YEARLY` = 20.
Các chủ đề chưa có game tự hiển thị "Sắp ra mắt".

### Danh sách game hiện có
Trí tuệ: Cờ vua, Cờ tướng, Cờ vây 19x19, Quoridor (2 hoặc 4 người), Wall Go (2-4 người), Cờ caro, Lãnh địa đồng cỏ (core `Meadow.kt`: 2-8 người, bàn 30×30, 96 mảnh polyomino theo hàng chờ cố định `ORDER`, nước đi gồm cả bước phụ không hết lượt như dùng phiếu, mua phiếu, dọn đá, và các pha đặt đá / cướp mảnh; bot `MeadowBot` đánh giá một lớp theo tiềm năng hình vuông; app `MeadowBoard.kt`, hết 2 phút thì tự bỏ mảnh thay vì xử thua), Khối gỗ ghép chữ (core `WordBlocks.kt`: 15 chủ đề từ tiếng Anh 3-8 chữ cái, đề = chữ cái của 7 từ xáo trộn, chỉ nhận đề có đúng một bộ 7 từ trong chủ đề dùng vừa hết các khối (`countSolutions`), game chỉ nhận 7 từ đáp án; màn hình `WordBlocksScreen.kt`). Giải đố: Sudoku, Ma phương 7×7 (mỗi ngày một đề), Xếp hình Tangram (core `Tangram.kt`: 7 cấp theo `LEVEL_KINDS` 4 → 10 mảnh, mở khóa như cấp bot; sinh đề ngẫu nhiên bằng cách áp cạnh 7 mảnh, độ khó theo độ đặc diện tích / bao lồi, chấm bằng lưới điểm nên chấp nhận mọi cách ghép; màn hình `TangramScreen.kt`). Ghi nhớ: Cờ caro mù (core `BlindCaro.kt` bọc `GomokuGame`, mã nước có thêm 2 bit màu; bot và bảng cấp dùng nguyên của caro; bàn cờ `BlindCaroBoard.kt`). Cờ vây dùng luật diện tích Tromp-Taylor (không gỡ quân chết tự động); bot cờ vây là KataGo (tiến trình GTP `libkatago.so`, xem `match/KataGo.kt` và `tools/katago/build_android.sh`), dự phòng bằng MCTS-RAVE của core khi thiết bị không chạy được KataGo. Trang chủ đề (`TopicScreen.kt`) xếp game theo `GameInfo.popularity` tăng dần (game độc lạ lên trước) và có hàng thẻ `Tag` (nội dung + một thẻ thời lượng tự suy từ `minutes`); chọn thẻ thì game có thẻ đó lên đầu (`Catalog.byTopicTagFirst`). Trang chủ có ô tìm kiếm không phân biệt dấu (`HomeSearch.kt`) và hàng game yêu thích (`Prefs.favorites`, chỉ khi có gói tháng/năm). Thưởng thắng: `Wallet.finishMatch(base)`, `SessionViewModel.winBase()` = cấp vừa thắng khi đấu máy (Dễ 0 ... Siêu việt 6), còn lại 1. Màn chơi bật `FLAG_SECURE` (không cho chụp màn hình), các màn khác thì cho.

### Bot 7 cấp
Mọi game từ 2 người trở lên có 7 cấp bot (Dễ ... Siêu việt) theo [`BOT_GUIDE.md`](BOT_GUIDE.md). Bảng cấu hình duy nhất nằm ở `core/.../BotLevels.kt`; engine cài `LeveledEngine.levelMove(level, botMillis, rnd)`. Người chơi bắt đầu ở cấp Dễ, thắng cấp L mở cấp L + 1 (`Prefs.unlockLevel`). Thời gian mỗi nước khi đấu máy theo cấp ở `Catalog.LEVEL_MOVE_SECONDS`. Đo 7 cấp và so biến thể engine chạy trên GitHub Actions (workflow `bot-bench.yml`, mã ở `core/src/test/.../bench/`, hướng dẫn trong [`tools/bot-bench/README.md`](../tools/bot-bench/README.md)); tinh chỉnh Texel cờ vua / cờ tướng chạy bằng workflow `tune.yml`. Mỗi đợt đo lưu thành một tệp `tools/bot-bench/results/<đợt>.txt` (dòng `# date:`, `# note:` rồi các dòng kết quả); chạy `python tools/bot-bench/make_reports.py` để cập nhật báo cáo [`docs/bot-results/`](bot-results/README.md) (số đợt, số ván, cặp đạt, chỉ số thông minh từng cấp) và `make_profiles.py` cho hồ sơ `docs/games/`.

### Luật lặp thế của app
Cờ vua, cờ tướng, Quoridor 2 người: nước tạo lại thế đã có (cùng bên tới lượt) thì bên vừa đi thua (`EndReason.REPETITION`). Quoridor 4 người: nước đó bị cấm. Wall Go 3 và 4 người dùng số quân mỗi người (3 và 2) là quy ước của app, không phải luật chính thức.

Gói đăng ký, kim cương và quảng cáo: xem [`MONETIZATION.md`](MONETIZATION.md). Giao diện và phông chữ: [`DESIGN.md`](DESIGN.md).

## 5. Cơ chế chống gian lận (mức 2)
* **Ví:** kim cương / thống kê mã hóa **AES-256-GCM**, khóa nằm trong **Android Keystore**; sửa file là giải mã thất bại → ví về trạng thái ban đầu. Bộ đếm phụ (`wallet.ctr`) chống khôi phục file cũ. Auto Backup chỉ sao lưu `pm_marks.xml` (số lượt miễn phí đã dùng); ví và khóa không bao giờ được sao lưu. Bản sao ví có chữ ký ở Google Block Store chống "gỡ app cài lại là có lượt miễn phí" (xem `MONETIZATION.md`).
* **Sửa bộ nhớ** (GameGuardian...): `WalletRepo` giữ bản sao số kim cương đã che bằng khóa ngẫu nhiên kèm mã kiểm tra. Số trong bộ nhớ lệch bản sao → dùng lại giá trị đúng và tạm dừng thưởng trong lần chạy đó.
* **Chỉnh giờ:** `NetTime` lấy giờ chuẩn từ header `Date` của máy chủ Google, neo vào `elapsedRealtime`; khi offline thì dùng đồng hồ máy có phát hiện nhảy giờ. Ngày điểm danh / lượt miễn phí ghi ở "tương lai" không mở thêm lượt.
* **Giả lập thanh toán** (Lucky Patcher, "freedom"): `PurchaseCheck` kiểm tra chữ ký giao dịch bằng khóa công khai RSA của app và tên gói; giao dịch giả bị bỏ qua. Cần biến `PLAY_LICENSE_KEY` (GitHub → Settings → Secrets and variables → Actions → Variables), lấy ở Play Console → Kiếm tiền bằng Play → Thiết lập kiếm tiền → Cấp phép. Kim cương chỉ được cộng sau khi Google Play tiêu thụ giao dịch thành công.
* **Sửa app rồi ký lại:** `Integrity` so chứng chỉ ký với danh sách hợp lệ (khóa tải lên lấy từ keystore trên CI + khóa ký của Google Play). Bật khi có biến `PLAY_CERT_SHA256` (Play Console → Kiểm tra và phát hành → Thiết lập → Tính toàn vẹn của ứng dụng → Chứng chỉ khóa ký ứng dụng, dòng SHA-256).
* **Môi trường:** root (su, Magisk, KernelSU), debugger, tiến trình bị theo dõi, thư viện hook (Frida, Xposed, LSPosed, Riru, Zygisk, Substrate), app nhân bản / không gian ảo, bản build test-keys → **tạm dừng nhận kim cương thưởng** (thắng ván, chuỗi thắng, điểm danh), game và mua hàng vẫn bình thường.
* **Ván đấu:** ván dở khi app bị tắt tính là thua; online mọi nước đi được engine kiểm tra ở cả hai máy, ván thắng chỉ được thưởng khi hai máy đối chiếu cùng kết quả (băm danh sách nước đi); đầu hàng / thoát quá sớm không được thưởng.
* **R8:** làm rối mã, dồn các lớp vào một gói, bỏ mọi lệnh Logcat ở bản phát hành.

Giới hạn: app không có máy chủ riêng, nên người có root và công cụ hook chuyên dụng vẫn có thể can thiệp vào chính máy của họ (ví dụ khôi phục cả thư mục dữ liệu). Mục tiêu là nâng chi phí gian lận và giữ kim cương thưởng không bị lạm dụng. Bước tiếp theo nếu cần mạnh hơn: Play Integrity API kèm Cloud Function giải mã token (cần gói Firebase Blaze), và lưu ví trên máy chủ.
