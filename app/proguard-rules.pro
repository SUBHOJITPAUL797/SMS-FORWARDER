# Proguard rules for SMS Bridge

# Firebase
-keepattributes *Annotation*
-dontwarn com.google.firebase.**
-keep class com.google.firebase.** { *; }

# Models
-keep class com.example.domain.model.** { *; }
-keep class com.example.data.local.** { *; }

# Coroutines & Room
-keep class androidx.room.** { *; }
-dontwarn androidx.room.**
