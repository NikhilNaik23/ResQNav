# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Keep line numbers for better crash reports
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Keep generic signature information for proper type inference
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod

# ========== Google Play Services & Firebase Components ==========
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**

-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

-keep class com.google.firebase.components.** { *; }
-keep interface com.google.firebase.components.** { *; }

# ========== Firebase Rules ==========
# Firebase KTX - Critical for R8
-keep class com.google.firebase.ktx.Firebase { *; }
-keep class com.google.firebase.ktx.** { *; }
-keep interface com.google.firebase.ktx.** { *; }
-keep class com.google.firebase.FirebaseApp { *; }
-keep class com.google.firebase.FirebaseOptions { *; }
-keep class com.google.firebase.FirebaseCommonKtxRegistrar { *; }
-dontwarn com.google.firebase.ktx.**

# Firebase Common
-keep class com.google.firebase.** { *; }
-keep interface com.google.firebase.** { *; }
-keepclassmembers class com.google.firebase.** { *; }

# Firebase Firestore KTX
-keep class com.google.firebase.firestore.ktx.** { *; }
-keep class com.google.firebase.firestore.FirebaseFirestore { *; }
-keep class com.google.firebase.firestore.FirebaseFirestoreSettings { *; }

# Firebase Auth KTX
-keep class com.google.firebase.auth.ktx.** { *; }
-keep class com.google.firebase.auth.FirebaseAuth { *; }
-keep class com.google.firebase.auth.FirebaseUser { *; }

# Firebase Messaging KTX
-keep class com.google.firebase.messaging.ktx.** { *; }
-keep class com.google.firebase.messaging.FirebaseMessaging { *; }
-keep class com.google.firebase.messaging.RemoteMessage { *; }

# Firebase Analytics KTX
-keep class com.google.firebase.analytics.ktx.** { *; }
-keep class com.google.firebase.analytics.FirebaseAnalytics { *; }

-keepclassmembers class * {
    @com.google.firebase.firestore.PropertyName <fields>;
    @com.google.firebase.firestore.PropertyName <methods>;
}

# Keep Firebase model classes
-keep class com.resqnav.app.models.** { *; }
-keep class com.resqnav.app.data.** { *; }

# Firebase Auth
-keepnames class com.google.firebase.auth.** { *; }

# Firebase Firestore
-keep class com.google.firebase.firestore.** { *; }
-keepclassmembers class * {
    @com.google.firebase.firestore.** <fields>;
}

# Firebase Messaging
-keep class com.google.firebase.messaging.** { *; }

# ========== Retrofit & OkHttp Rules ==========
# Retrofit does reflection on generic parameters
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepattributes AnnotationDefault

-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# Ignore annotation used for build tooling
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement

# Ignore JSR 305 annotations for embedding nullability information
-dontwarn javax.annotation.**

# Guarded by a NoClassDefFoundError try/catch
-dontwarn kotlin.Unit

# Top-level functions that can only be used by Kotlin
-dontwarn retrofit2.KotlinExtensions
-dontwarn retrofit2.KotlinExtensions$*

# With R8 full mode, it sees no subtypes of Retrofit interfaces
-if interface * { @retrofit2.http.* <methods>; }
-keep,allowobfuscation interface <1>

# Keep generic signature of Call, Response (R8 full mode strips signatures)
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response

# With R8 full mode generic signatures are stripped
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# ========== Gson Rules ==========
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Keep your data models
-keep class com.resqnav.app.network.** { <fields>; }

# ========== TensorFlow Lite Rules ==========
-keep class org.tensorflow.lite.** { *; }
-keep interface org.tensorflow.lite.** { *; }
-dontwarn org.tensorflow.lite.**

# TensorFlow Lite Support
-keep class org.tensorflow.lite.support.** { *; }
-dontwarn org.tensorflow.lite.support.**

# ========== ML Kit Rules ==========
-keep class com.google.mlkit.** { *; }
-keep interface com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**

# ========== Gemini AI Rules ==========
-keep class com.google.ai.client.generativeai.** { *; }
-keep interface com.google.ai.client.generativeai.** { *; }
-dontwarn com.google.ai.client.generativeai.**

# ========== Room Database Rules ==========
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# ========== OSMDroid Rules ==========
-dontwarn org.osmdroid.**
-keep class org.osmdroid.** { *; }

# ========== Kotlin Coroutines Rules ==========
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
-keepclassmembers class kotlin.coroutines.SafeContinuation {
    volatile <fields>;
}
-dontwarn kotlinx.coroutines.**

# ========== CameraX Rules ==========
-keep class androidx.camera.** { *; }
-dontwarn androidx.camera.**

# ========== AndroidX Rules ==========
-keep class androidx.lifecycle.** { *; }
-keep class androidx.core.** { *; }

# ========== Native Methods ==========
-keepclasseswithmembernames class * {
    native <methods>;
}

# ========== Parcelable & Serializable ==========
-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# ========== Enums ==========
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ========== R8 Optimizations ==========
# Remove logging in release builds
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}

# Keep crash reporting intact
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keep public class * extends java.lang.Exception
