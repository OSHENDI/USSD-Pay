# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Preserve line numbers and source file names for diagnostics
-keepattributes SourceFile,LineNumberTable,*Annotation*,Signature,InnerClasses,EnclosingMethod

# Room Database rules
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keep class * implements androidx.room.RoomDatabase$Callback { *; }
-keepclassmembers class * {
    @androidx.room.* *;
}

# Protect all application models, managers, and data classes (avoids reflection/serialization failure)
-keep class com.example.** { *; }
-keepclassmembers class com.example.** { *; }

# Protect ZXing Barcode / QR classes
-keep class com.google.zxing.** { *; }
-dontwarn com.google.zxing.**

# Protect CameraX classes
-keep class androidx.camera.** { *; }
-dontwarn androidx.camera.**

# Protect Google Play In-App Updates
-keep class com.google.android.play.core.** { *; }

# Jetpack Compose and Coroutines optimizations
-dontwarn com.google.errorprone.annotations.**

