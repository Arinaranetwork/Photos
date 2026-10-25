# Proguard rules for Photos
-keepattributes *Annotation*
-keepclassmembers class * {
    @androidx.room.* *;
}
