# ============================================================
# seed-mas release rules
# ============================================================
# این فایل با R8 (minifyEnabled true) در buildType.release فعال است.
# هدف: جلوگیری از حذف یا مبهم‌سازی کلاس‌هایی که از طریق reflection،
# JNI یا ServiceLoader بارگذاری می‌شوند (ML Kit، CameraX، Parcelable، ...).

# ---------- ML Kit ----------
# ML Kit به شدت وابسته به reflection و JNI است. کلاس‌های آن باید حفظ شوند.
-keep class com.google.mlkit.** { *; }
-keep class com.google.android.gms.internal.mlkit_** { *; }
-keep class com.google.android.gms.internal.mlkit_vision_** { *; }
-keep class com.google.mlkit.vision.text.** { *; }
-keep class com.google.mlkit.vision.barcode.** { *; }
-keep class com.google.mlkit.vision.common.** { *; }

# حفظ امضای native method ها (الزامی برای JNI)
-keepclasseswithmembernames class * {
    native <methods>;
}

# ---------- CameraX ----------
# CameraX از reflection برای کشف provider و UseCase استفاده می‌کند.
-keep class androidx.camera.** { *; }
-keep interface androidx.camera.** { *; }
-keep class * extends androidx.camera.core.UseCase { *; }
-keep class * implements androidx.camera.core.CameraProvider { *; }

# ---------- AndroidX / Android ----------
# سازنده View های سفارشی (برای inflate از XML)
-keepclasseswithmembers class * extends android.view.View {
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
}

# Parcelable CREATOR (برای جلوگیری از BadParcelableException)
-keepclassmembers class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# متدهای enum (بعضی کتابخانه‌ها با reflection صدا می‌زنند)
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ---------- Kotlin Coroutines ----------
-keep class kotlinx.coroutines.** { *; }
-keepclassmembers class kotlin.coroutines.** { *; }

# ---------- اطلاعات دیباگ ----------
# حفظ شماره خط برای خوانایی stack trace پس از انتشار
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
