plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.a.kappa"
    compileSdk = 35


    packagingOptions {
        exclude("META-INF/DEPENDENCIES")
    }


    defaultConfig {
        applicationId = "com.a.kappa"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    // WebDAV клієнт (без commons-logging)
    implementation("org.apache.jackrabbit:jackrabbit-webdav:2.21.4") {
        exclude(group = "commons-logging", module = "commons-logging")
    }
    // HTTP клієнт
    implementation(libs.okhttp.v493)

    //бібліотека для навігації між екранами в Jetpack Compose.
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.material3)


    // AndroidX бібліотеки
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)

    // Тестування
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
