import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
}

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(FileInputStream(localPropertiesFile))
}

val mapsApiKey: String = run {
    val envKey = System.getenv("MAPS_API_KEY")
    if (!envKey.isNullOrBlank()) envKey
    else localProperties.getProperty("MAPS_API_KEY") ?: ""
}

android {
    namespace = "com.example.gymsync"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.example.gymsync"
        minSdk = 23
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        resValue("string", "google_maps_key", mapsApiKey)
        buildConfigField("String", "MAPS_API_KEY", "\"$mapsApiKey\"")
    }

    buildFeatures {
        buildConfig = true
        resValues = true
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.activity.ktx)
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)
    implementation("com.google.android.gms:play-services-maps:19.2.0")
    implementation("com.google.android.libraries.places:places:4.3.1")
}