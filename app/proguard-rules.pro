# WebRTC dùng JNI nên phải giữ nguyên tên lớp/phương thức
-keep class org.webrtc.** { *; }
-dontwarn org.webrtc.**
# Giữ thông tin dòng lệnh để đọc stack trace đã deobfuscate (mapping.txt)
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
-dontwarn com.google.errorprone.annotations.**
-dontwarn javax.annotation.**
