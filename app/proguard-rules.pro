# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Preserve attributes for reflection, annotations, and generic signatures
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod,SourceFile,LineNumberTable

# Keep all Domain Models & Data Transfer Objects (Crucial for Firestore & JSON persistence)
-keep class com.example.domain.model.** { *; }
-keepclassmembers class com.example.domain.model.** { *; }
-keep class com.example.data.** { *; }
-keepclassmembers class com.example.data.** { *; }

# Firebase Core, Auth, Firestore & Analytics Rules
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**
-keepclassmembers class * {
    @com.google.firebase.firestore.PropertyName <fields>;
    @com.google.firebase.firestore.PropertyName <methods>;
    @com.google.firebase.firestore.Exclude <fields>;
    @com.google.firebase.firestore.Exclude <methods>;
    @com.google.firebase.firestore.IgnoreExtraProperties <fields>;
}

# Kotlin Coroutines & Serialization
-keepclassmembers class * extends kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# Android Credential Manager & Google Sign-In
-keep class androidx.credentials.** { *; }
-keep class com.google.android.libraries.identity.googleid.** { *; }

