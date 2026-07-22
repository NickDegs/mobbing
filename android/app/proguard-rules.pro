# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class com.nickdegs.mobbing.**$$serializer { *; }
-keepclassmembers class com.nickdegs.mobbing.** { *** Companion; }
-keepclasseswithmembers class com.nickdegs.mobbing.** { kotlinx.serialization.KSerializer serializer(...); }
