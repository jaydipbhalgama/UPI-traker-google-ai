# Proguard / R8 rules for UPI Tracker

# Keep Room generated code and annotations
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Keep entities and DAOs
-keep class com.example.upitracker.data.local.entity.** { *; }
-keep interface com.example.upitracker.data.local.dao.** { *; }

# Kotlin Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.SerializationKt
-keepclassmembers class * {
    *** Companion;
}
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep Compose models and viewmodels if needed
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}
