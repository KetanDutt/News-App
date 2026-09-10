# ════════════════════════════════════════════════════════════════════
# News App R8 / ProGuard rules (release builds)
# Retrofit, OkHttp and Coil ship their own consumer rules; the rules
# below keep the reflection-based Gson models intact.
# ════════════════════════════════════════════════════════════════════

# Keep generics & annotation metadata required by Gson/Retrofit.
-keepattributes Signature, *Annotation*, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations

# JSON model classes (parsed reflectively by Gson).
-keep class com.rtctek.newsapp.retrofit.** { *; }
-keep class com.rtctek.newsapp.NewsModel { *; }

# Retrofit interface methods annotated with @GET/@Query etc.
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation
-keep,allowobfuscation,allowshrinking class retrofit2.Response

# Silence warnings from optional dependencies.
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-dontwarn org.codehaus.mojo.animal_sniffer.*
