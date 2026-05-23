// File top-level: dichiara i plugin disponibili per i moduli, senza applicarli.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android)      apply false
    alias(libs.plugins.kotlin.compose)      apply false
    alias(libs.plugins.ksp)                 apply false
    alias(libs.plugins.hilt.android)        apply false
    alias(libs.plugins.google.services)     apply false
}
