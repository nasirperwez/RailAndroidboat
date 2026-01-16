# Add project specific ProGuard rules here.

# Kotlin Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.example.railchatbot.**$$serializer { *; }
-keepclassmembers class com.example.railchatbot.** {
    *** Companion;
}
-keepclasseswithmembers class com.example.railchatbot.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Ktor
-keep class io.ktor.** { *; }
-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.atomicfu.**
-dontwarn io.netty.**
-dontwarn com.typesafe.**
-dontwarn org.slf4j.**

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# OpenAI SDK
-keep class com.aallam.openai.** { *; }

# MCP SDK
-keep class io.modelcontextprotocol.** { *; }

# Keep data classes
-keep class com.example.railchatbot.data.remote.mcp.model.** { *; }
-keep class com.example.railchatbot.data.remote.ai.model.** { *; }
-keep class com.example.railchatbot.domain.model.** { *; }
