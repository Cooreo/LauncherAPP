# Keep kotlinx.serialization generated classes
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.roomlauncher.app.data.**$$serializer { *; }
-keepclassmembers class com.roomlauncher.app.data.** {
    *** Companion;
}
-keepclasseswithmembers class com.roomlauncher.app.data.** {
    kotlinx.serialization.KSerializer serializer(...);
}
