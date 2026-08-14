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
        versionCode = 7
        versionName = "1.0.6"
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
            configure<CrashlyticsExtension> {
                mappingFileUploadEnabled = true
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

    // Override outdated transitive AndroidX deps pulled in by firebase-analytics →
    // play-services-measurement (Play Console flagged fragment 1.1.0 / activity 1.0.0).
    implementation(libs.androidx.fragment)
    implementation(libs.androidx.activity)
    // Direct dep for WindowCompat / WindowInsetsControllerCompat used in GDActivity.applyImmersiveMode().
    implementation(libs.androidx.core)
}
