plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)   // Kotlin 2.x: Compose compiler è bundled, niente versione separata
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.google.services)
}

android {
    namespace  = "com.app.notespese"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.app.notespese"
        minSdk        = 29
        targetSdk     = 35
        versionCode   = 1
        versionName   = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        vectorDrawables { useSupportLibrary = true }
    }

    signingConfigs {
        // Keystore stabile condiviso tra CI e sviluppo locale.
        // SHA-1: 1C:82:32:0E:23:B9:AB:41:4C:9D:AD:FA:F7:4F:36:49:34:31:79:A2
        // SHA-256: 96:D9:32:3B:F1:71:F3:DB:BE:13:97:52:F8:DF:98:03:DF:15:00:6C:E2:8E:BE:63:F7:3D:45:B4:FA:67:12:73
        // Registra entrambi su Firebase Console > Project Settings > Android app.
        getByName("debug") {
            storeFile     = file("debug.keystore")
            storePassword = "android"
            keyAlias      = "androiddebugkey"
            keyPassword   = "android"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled   = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isDebuggable      = true
            versionNameSuffix = "-debug"
            signingConfig     = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        // Java 17: richiesto da Kotlin 2.x e consigliato per AGP 8.x
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose     = true
        buildConfig = true  // permette BuildConfig.DEBUG e costanti custom
    }

    packaging {
        resources {
            // Licenze duplicate introdotte da numerose librerie (Firebase, POI, ecc.)
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/DEPENDENCIES"
            excludes += "/META-INF/LICENSE"
            excludes += "/META-INF/LICENSE.txt"
            excludes += "/META-INF/NOTICE"
            excludes += "/META-INF/NOTICE.txt"
            // Richiesto da Apache POI / xmlbeans
            excludes += "mozilla/public-suffix-list.txt"
        }
    }
}

dependencies {

    // ── AndroidX Core ────────────────────────────────────────────────────────
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)

    // ── Jetpack Compose ───────────────────────────────────────────────────────
    // Il BOM allinea tutte le versioni androidx.compose.* senza specificarle singolarmente.
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)

    // ── Navigation ────────────────────────────────────────────────────────────
    implementation(libs.androidx.navigation.compose)

    // ── Hilt (DI) ─────────────────────────────────────────────────────────────
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)
    implementation(libs.hilt.navigation.compose)
    // hilt-work integra Hilt con WorkManager per i Worker iniettabili
    implementation(libs.hilt.work)
    ksp(libs.hilt.compiler.androidx)

    // ── Firebase ──────────────────────────────────────────────────────────────
    // Il BOM gestisce le versioni: non serve version.ref sui singoli artefatti.
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.messaging)
    implementation(libs.firebase.storage)
    implementation(libs.firebase.analytics)
    // Credential Manager: API moderna per Google Sign-In (Android 9+)
    // credentials-play-services-auth garantisce backward compat via Play Services
    implementation(libs.credentials)
    implementation(libs.credentials.play.services)
    implementation(libs.googleid)

    // ── Room (cache offline) ──────────────────────────────────────────────────
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // ── Coroutines ────────────────────────────────────────────────────────────
    implementation(libs.kotlinx.coroutines.android)
    // play-services fornisce .await() per Task<T> di Firebase/GMS
    implementation(libs.kotlinx.coroutines.play.services)

    // ── Coil (caricamento immagini in Compose) ────────────────────────────────
    implementation(libs.coil.compose)

    // ── Glance (widget home screen) ───────────────────────────────────────────
    implementation(libs.glance.appwidget)
    implementation(libs.glance.material3)

    // ── WorkManager (ricorrenze, reminder debiti) ─────────────────────────────
    implementation(libs.work.runtime.ktx)

    // ── DataStore Preferences (impostazioni locali) ───────────────────────────
    implementation(libs.datastore.preferences)

    // ── Apache POI (export XLSX) ──────────────────────────────────────────────
    // xmlbeans 5.x non ha dipendenze AWT, quindi gira su Android senza problemi.
    // I file META-INF duplicati sono esclusi nel blocco packaging sopra.
    implementation(libs.poi.ooxml)
    implementation(libs.xmlbeans)

    // ── ML Kit Text Recognition (OCR scontrini) ───────────────────────────────
    implementation(libs.mlkit.text.recognition)

    // ── Test ──────────────────────────────────────────────────────────────────
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
