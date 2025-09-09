plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    kotlin("plugin.serialization") version "2.2.0"
}

android {
    namespace = "dev.mooner.eevee"
    compileSdk = 34

    defaultConfig {
        applicationId = "dev.mooner.eevee"
        minSdk = 21
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {

    implementation("androidx.core:core-ktx:1.13.1") //
    implementation("androidx.appcompat:appcompat:1.6.1") //
    implementation("com.google.android.material:material:1.12.0") //
    implementation("androidx.activity:activity:1.9.3") //
    implementation("androidx.databinding:viewbinding:8.13.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4") //
    implementation("androidx.navigation:navigation-fragment-ktx:2.9.3")
    implementation("androidx.navigation:navigation-ui-ktx:2.9.3")
    implementation("androidx.recyclerview:recyclerview:1.3.2") //
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
    implementation("com.google.android.gms:play-services-location:21.3.0")

    implementation("io.coil-kt.coil3:coil:3.0.4") //
    implementation("com.github.Dimezis:BlurView:version-3.1.0")
    implementation("pl.droidsonroids.gif:android-gif-drawable:1.2.29")
    implementation("com.guolindev.permissionx:permissionx:1.8.1")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
}