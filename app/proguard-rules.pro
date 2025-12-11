# PROGUARD SIMPLE
-dontobfuscate
-dontoptimize
-dontpreverify

# Keep semua class app
-keep class com.example.login.** { *; }

# Keep libraries penting
-keep class com.google.gson.** { *; }
-keep class retrofit2.** { *; }
-keep class okhttp3.** { *; }

# Nonaktifkan semua warning
-dontwarn **