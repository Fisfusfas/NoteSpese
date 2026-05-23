# Mantieni informazioni di debug per stack trace leggibili nel crash reporting
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ── Kotlin ────────────────────────────────────────────────────────────────────
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**

# ── Firebase Firestore ────────────────────────────────────────────────────────
# I data class Kotlin mappati da Firestore devono mantenere campi e costruttori,
# altrimenti la deserializzazione fallisce silenziosamente a runtime.
-keep class com.app.notespese.data.model.** { *; }
-keepclassmembers class com.app.notespese.data.model.** { *; }

# Regole ufficiali Firebase (incluse automaticamente dal plugin google-services,
# ma le ribadiamo per evitare problemi con ProGuard standalone)
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# ── Hilt ──────────────────────────────────────────────────────────────────────
# Hilt genera classi _HiltComponents, _MembersInjector, ecc. a compile time:
# R8 le conosce già, ma questa regola è un safety net per edge case.
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-dontwarn dagger.**

# ── Room ──────────────────────────────────────────────────────────────────────
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao interface *

# ── Apache POI / xmlbeans ────────────────────────────────────────────────────
# POI usa riflessione su SchemaType; senza queste regole la generazione XLSX
# fallisce con ClassNotFoundException a runtime.
-keep class org.apache.poi.** { *; }
-keep class org.apache.xmlbeans.** { *; }
-dontwarn org.apache.poi.**
-dontwarn org.apache.xmlbeans.**
-dontwarn org.openxmlformats.**
-dontwarn org.etsi.**
-dontwarn org.w3.**

# ── ML Kit ────────────────────────────────────────────────────────────────────
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**

# ── Kotlin Coroutines ────────────────────────────────────────────────────────
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-dontwarn kotlinx.coroutines.**

# ── Jetpack Compose ───────────────────────────────────────────────────────────
# Compose runtime usa riflessione su @Composable e @Stable internamente.
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# ── Coil ──────────────────────────────────────────────────────────────────────
-dontwarn coil.**

# ── WorkManager ───────────────────────────────────────────────────────────────
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.CoroutineWorker
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
