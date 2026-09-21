# ProGuard rules for Poetry App

# Keep Room entities
-keep class com.nalansitan.chinesepoetry.data.local.entity.** { *; }

# Keep model classes
-keep class com.nalansitan.chinesepoetry.domain.model.** { *; }

# Hilt
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.internal.GeneratedComponent { *; }

# Room
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Paging
-keep class androidx.paging.** { *; }
