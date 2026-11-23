# Sổ tay sửa lỗi build

Tổng hợp các lỗi đã gặp khi build **Play Mind** trên GitHub Actions và cách sửa. Khi thêm game mới, hãy đọc mục 1 trước, vì hầu hết lỗi lặp lại theo kiểu.

## 1. Cách đọc log CI

1. Mở job **build** → bước đang đỏ → tìm dòng đầu tiên bắt đầu bằng `e:` (lỗi Kotlin) hoặc `error:` (AAPT/Lint).
2. **Chỉ sửa lỗi đầu tiên của mỗi file.** Các dòng `Cannot infer type`, `Unresolved reference`, `Argument type mismatch` ngay sau đó thường là *hệ quả* của cùng một nguyên nhân.
3. Mỗi lần push, CI chạy lại toàn bộ nên hãy sửa **tất cả** lỗi trong log rồi mới push.
4. Muốn lặp nhanh trên máy có Android SDK: `gradle :app:compileDebugKotlin` (chỉ biên dịch, mất khoảng 1–2 phút).
5. Phần logic game nằm trong `:core` (Kotlin thuần) nên kiểm chứng rất nhanh bằng `gradle :core:selfCheck` hoặc `kotlinc` mà không cần Android SDK.

Mẹo tách lỗi: nếu log báo hàng chục lỗi ở nhiều file nhưng đều liên quan một hàm Compose, sửa hàm đó trước.

## 2. Lỗi đã gặp và cách sửa

### 2.1 `Argument type mismatch: actual type is 'Modifier', but 'Function1<Context, T>' was expected` (Home.kt)

```
Home.kt:75:5  Cannot infer type for this parameter. Specify it explicitly.
Home.kt:76:9  Argument type mismatch: actual type is 'Modifier', but 'Function1<Context, uninferred T ...AndroidView>' was expected.
Home.kt:77:9  Argument already passed for this parameter.
Home.kt:84:26 Unresolved reference 'destroy'.
```

**Nguyên nhân:** tham số đầu tiên của `AndroidView` là `factory`, **không phải** `modifier` (khác với hầu hết composable khác). Truyền `Modifier` vào vị trí đầu gây ra một chuỗi lỗi suy luận kiểu.

**Sửa:** truyền theo tên và nêu rõ kiểu view.

```kotlin
AndroidView<AdView>(
    factory = { context -> AdView(context).apply { /* ... */ } },
    modifier = modifier.fillMaxWidth(),
    onRelease = { adView -> adView.destroy() },
)
```

**Bài học chung:** với API Compose lạ, luôn truyền tham số **theo tên**, nhất là `modifier`, `onClick`, `content`.

### 2.2 `Function invocation 'to(...)' expected` / `operator '==' cannot be applied to 'Pair<...>'`

Đặt hàm tên `to`, `from`, `with`, `let`... trong `companion object` của một lớp rồi gọi từ bên trong lớp: Kotlin ưu tiên hàm mở rộng của stdlib (`infix fun A.to(B): Pair`) nên `to(m)` trở thành `Pair(this, m)`.

**Sửa:** đặt tên không trùng stdlib (`fromSq`, `toSq`, `promoOf`).

### 2.3 Gradle: cấu hình đặt sai chỗ

| Sai | Đúng |
|---|---|
| `androidResources { localeFilters ... }` bên trong `defaultConfig { }` | đặt ở cấp `android { androidResources { localeFilters += listOf(...) } }` |
| `kotlin { jvmToolchain(17) }` trong module JVM (máy khác không có JDK 17 → lỗi tải toolchain) | `java { sourceCompatibility = 17; targetCompatibility = 17 }` + `kotlin { compilerOptions { jvmTarget.set(JVM_17) } }` |
| Thiếu `gradle-wrapper.jar` nên `./gradlew` hỏng | CI dùng `gradle/actions/setup-gradle` với `gradle-version`, hoặc chạy `gradle wrapper` một lần rồi commit |

### 2.4 Cảnh báo GitHub Actions: Node.js 20 / `setup-java` v4 deprecated

Nâng `actions/checkout@v5` và `actions/setup-java@v5`. Đây chỉ là cảnh báo, build vẫn chạy, nhưng nên sửa để khỏi hỏng khi Node 20 bị gỡ.

### 2.5 R8 / thu nhỏ mã làm app crash ở bản release

Dấu hiệu: bản debug chạy tốt, bản release crash `ClassNotFoundException`/`UnsatisfiedLinkError`.
Nguyên nhân thường gặp: thư viện dùng JNI hoặc reflection (WebRTC). Đã có trong `app/proguard-rules.pro`:

```
-keep class org.webrtc.** { *; }
-dontwarn org.webrtc.**
```

Thêm thư viện mới dùng JNI/reflection thì thêm rule `-keep` tương ứng. Dùng `mapping.txt` (CI tải kèm) để đọc stack trace đã làm rối.

### 2.6 Artifacts chiếm dung lượng

Mọi bước `upload-artifact` đặt `retention-days: 1` để Artifacts tự xóa sau 1 ngày. Muốn áp dụng cho cả repo: *Settings → Actions → General → Artifact and log retention*.

## 3. Checklist trước khi push một game mới

- [ ] `gradle :core:selfCheck` xanh (perft/tình huống/AI của game mới).
- [ ] `gradle :app:compileDebugKotlin` xanh.
- [ ] Mọi chuỗi mới có đủ 8 ngôn ngữ: `python3 tools/i18n/generate.py` báo `OK` (script tự kiểm tra thiếu khóa và đối chiếu với `R.string.*` trong mã).
- [ ] `GameInfo` đã thêm vào `Catalog.games` với `botMillis` hợp lý (xem `docs/BOT_GUIDE.md`).
- [ ] Bàn cờ mới đã nối vào `GameScreen` (`when (info.id)`) và `SideBadge`.
- [ ] Hướng dẫn chơi mới trong `docs/how-to-play/` (sửa `tools/build_docs.py` rồi chạy lại).
- [ ] Mô tả cửa hàng trong README vẫn ≤ 4.000 ký tự: `python3 tools/build_readme.py`.
