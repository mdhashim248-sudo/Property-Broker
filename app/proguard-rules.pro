# Property Broker — Google Play Release R8 / ProGuard Rules

# Preserve clean stack traces for Play Console crash reporting
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Keep all Firestore @Keep / @IgnoreExtraProperties data models and constructors
-keep @androidx.annotation.Keep class * { *; }
-keepclassmembers class com.example.data.model.** {
    public <init>(...);
    <fields>;
    <methods>;
}
-keep class com.example.data.model.** { *; }

# Keep Firebase Firestore, Auth, App Check, and AI Logic reflection hooks
-keepattributes Signature
-keepattributes *Annotation*
-keepclassmembers class * {
    @com.google.firebase.firestore.PropertyName <fields>;
    @com.google.firebase.firestore.PropertyName <methods>;
    @com.google.firebase.firestore.DocumentId <fields>;
    @com.google.firebase.firestore.ServerTimestamp <fields>;
}

# Keep Jetpack Credential Manager & Google Identity Services
-keep class androidx.credentials.** { *; }
-keep class com.google.android.libraries.identity.googleid.** { *; }

# Keep Kotlin Coroutines & Flow internals
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
