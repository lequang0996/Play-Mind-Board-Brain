# WebRTC dùng JNI nên phải giữ nguyên tên lớp/phương thức
-keep class org.webrtc.** { *; }
-dontwarn org.webrtc.**
# Giữ thông tin dòng lệnh để đọc stack trace đã deobfuscate (mapping.txt)
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
-dontwarn com.google.errorprone.annotations.**
-dontwarn javax.annotation.**

# Bản phát hành: bỏ mọi lệnh ghi Logcat (không lộ thông tin nội bộ cho người dò app)
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
    public static int e(...);
}
# Dồn các lớp đã đổi tên vào một gói, khó đọc cấu trúc app hơn khi dịch ngược
-repackageclasses ''
-allowaccessmodification
