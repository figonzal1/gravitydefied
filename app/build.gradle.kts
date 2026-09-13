import com.google.firebase.crashlytics.buildtools.gradle.CrashlyticsExtension
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
}

val keystorePropertiesFile = rootProject.file("keys/keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) load(keystorePropertiesFile.inputStream())
}

android {
    namespace = "cl.figonzal.gravitydefied"
    compileSdk = 37

    defaultConfig {
        applicationId = "cl.figonzal.gravitydefied"
        minSdk = 23
        targetSdk = 37
        versionCode = 10
        versionName = "1.0.9"
    }

    signingConfigs {
        create("release") {
            keyAlias = keystoreProperties["keyAlias"] as String
            keyPassword = keystoreProperties["keyPassword"] as String
            storeFile = file(keystoreProperties["storeFile"] as String)
            storePassword = keystoreProperties["storePassword"] as String
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    buildFeatures {
        resValues = true
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".dev"
            versionNameSuffix = "-debug"
            isDebuggable = true
            isMinifyEnabled = false
            resValue("string", "app_name", "Gravity Defied Classic-debug")
            // Local backend for world-ranking dev (see API/Ranking.java + src/debug's
            // network_security_config.xml cleartext override). Not used by release.
            resValue("string", "ranking_base_url", "http://192.168.1.161:3000")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = false
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            resValue("string", "app_name", "Gravity Defied Classic")
            resValue("string", "ranking_base_url", "https://ranking.gdtr.net")
            configure<CrashlyticsExtension> {
                // Only deploy builds upload the mapping (fastlane passes
                // -PuploadMapping); local release builds would otherwise keep
                // overwriting the mapping of the same versionCode in Firebase.
                mappingFileUploadEnabled = project.hasProperty("uploadMapping")
            }
        }
    }

    bundle {
        // Keep all density resources in the base install instead of density
        // config splits. Prevents Resources$NotFoundException ("Unable to find
        // resource ID") on devices where the density split is not installed
        // (sideloaded base APK / split delivery failures).
        density {
            enableSplit = false
        }
    }
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.analytics)

    // World ranking sign-in (Menu/RankingMenuScreen, API/Ranking) - PGS API surface only,
    // com.google.android.gms.games.* / .tasks.*, no androidx import (see GDActivity.java note below).
    implementation(libs.play.services.games)

    // Override outdated transitive AndroidX deps pulled in by firebase-analytics →
    // play-services-measurement (Play Console flagged fragment 1.1.0 / activity 1.0.0).
    implementation(libs.androidx.fragment)
    implementation(libs.androidx.activity)
    // Direct dep for WindowCompat / WindowInsetsControllerCompat used in GDActivity.applyImmersiveMode().
    implementation(libs.androidx.core)
}
