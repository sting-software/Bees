plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.hilt.android) apply false
    alias(libs.plugins.navigation.safeargs.kotlin) apply false
    id("com.google.gms.google-services") version "4.4.3" apply false
}