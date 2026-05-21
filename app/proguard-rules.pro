# ACRA crash reporting
-keep class org.acra.** { *; }
-keepattributes *Annotation*

# Apache HTTP client (used by API/)
-keep class org.apache.http.** { *; }
-dontwarn org.apache.http.**
-dontwarn android.net.http.**

# Game core — decompiled J2ME code, obfuscated identifiers must not be renamed
-keep class cl.figonzal.gravitydefied.Game.** { *; }
-keep class cl.figonzal.gravitydefied.Levels.** { *; }

# Keep all Activity, Application, and BroadcastReceiver entry points
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.content.BroadcastReceiver
