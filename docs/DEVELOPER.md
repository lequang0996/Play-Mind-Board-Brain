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
[Convert]::ToBase64String([System.IO.File]::ReadAllBytes("release.jks"))
```
Thêm vào *Settings → Secrets and variables → Actions → Secrets*: `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`. **Không commit file .jks.** Nên bật *Play App Signing* khi tạo app trên Play Console.

### ID AdMob thật
Mặc định dùng **ID thử nghiệm của Google** (an toàn khi phát triển). Trước khi phát hành, thêm *Variables*: `ADMOB_APP_ID`, `ADMOB_BANNER`, `ADMOB_INTERSTITIAL`, `ADMOB_REWARDED`. Không bấm vào quảng cáo thật của chính mình.

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
1. Tạo luật + AI trong `core/` (implement `BoardGame`) và thêm kiểm thử vào `SelfCheck.kt`.
2. Thêm một dòng `GameInfo(...)` vào `Catalog.games` (chọn `Topic`) và các chuỗi `game_*`, `guide_*` trong `tools/i18n/*.txt`, rồi chạy `python3 tools/i18n/generate.py`.
3. Thêm bàn cờ vào `ui/Boards.kt` và nhánh `when (info.id)` trong `GameScreen`.
Các chủ đề chưa có game tự hiển thị "Sắp ra mắt".

## 5. Cơ chế chống gian lận (mức 1)
* Ví kim cương/thống kê mã hóa **AES-256-GCM**, khóa nằm trong **Android Keystore**; sửa file là giải mã thất bại → ví về trạng thái ban đầu.
* Bộ đếm phụ (`wallet.ctr`) chống khôi phục file cũ; `allowBackup=false` + `dataExtractionRules` chặn nhân bản dữ liệu.
* Đồng hồ đáng tin (`elapsedRealtime`) phát hiện chỉnh giờ để lấy thêm điểm danh/lượt miễn phí.
* Ván đang chơi dở khi app bị tắt được tính là thua (chuỗi thắng về 0).
* Online: mọi nước đi được engine kiểm tra ở cả hai máy; ván thắng chỉ được thưởng khi hai máy đối chiếu cùng kết quả (băm danh sách nước đi).
* Môi trường: phát hiện root/debugger/Frida/Xposed → **tạm dừng nhận kim cương thưởng**, game vẫn chơi bình thường.
* R8 làm rối mã.

Giới hạn: không có server nên người dùng root + công cụ hook chuyên dụng vẫn có thể can thiệp; mục tiêu là nâng chi phí gian lận.
