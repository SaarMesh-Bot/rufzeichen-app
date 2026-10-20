# Readable stack traces in Play crash reports (deobfuscated via the bundled mapping file)
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# App data classes: Room entities and models (field names used by Room / JSON mapping)
-keep class de.hamlookup.rufzeichen.data.model.** { *; }
-keep class de.hamlookup.rufzeichen.data.local.** { *; }

# Jsoup
-keep class org.jsoup.** { *; }
-dontwarn org.jsoup.**

# osmdroid (tile sources and providers are partly created reflectively)
-keep class org.osmdroid.** { *; }
-dontwarn org.osmdroid.**

# OkHttp ships its own consumer rules; silence optional TLS providers
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
