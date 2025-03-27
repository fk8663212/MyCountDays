// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    id("org.jetbrains.kotlin.android") version "1.9.0" apply false
    // ksp (Kotlin Symbol Processing) devtools dependencies
    id("com.google.devtools.ksp") version "1.9.10-1.0.13" apply false
    //lias(libs.plugins.kotlin.compose) apply false
}
