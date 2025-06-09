plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.a.kappa"
    compileSdk = 35


    packagingOptions {
        exclude("META-INF/DEPENDENCIES")
        resources.excludes += setOf(
            "META-INF/INDEX.LIST",
            "META-INF/groovy/org.codehaus.groovy.runtime.ExtensionModule",
            "META-INF/groovy-release-info.properties"
        )
    }

    defaultConfig {
        applicationId = "com.a.kappa"
        minSdk = 26
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

configurations.all {
    exclude(group = "commons-logging", module = "commons-logging")
}

dependencies {
    // HTTP клієнт
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // Firebase для Push-повідомлень
    implementation(platform("com.google.firebase:firebase-bom:33.14.0")) // Версію можна оновити
    implementation(libs.firebase.messaging)
    implementation(libs.firebase.messaging.ktx)

    // AndroidX - Основа додатку
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
