# Keep libsu shell classes
-keep class com.topjohnwu.superuser.** { *; }

# Keep data models used for JSON profile export/import
-keep class com.fpsboostpro.app.data.model.** { *; }
-keepclassmembers class com.fpsboostpro.app.data.model.** { *; }

# Kotlinx serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
