# ProGuard rules for Blueth Guard

# Keep Hilt generated classes
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# Compose
-dontwarn androidx.compose.**

# Keep data classes used for serialization
-keepclassmembers class com.blueth.guard.data.model.** { *; }

# ---- Security hardening ----
# Strip debug info
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
}

# Obfuscate all app classes
-repackageclasses 'bg'
-allowaccessmodification

# Keep only what Room/Hilt need
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * { *; }

# Glance widget
-keep class com.blueth.guard.widget.** { *; }

# Kotlinx serialization
-keepattributes *Annotation*
-keep class kotlinx.serialization.** { *; }
-keepclassmembers class com.blueth.guard.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Navigation serialization routes
-keep class com.blueth.guard.ui.navigation.*Route { *; }
