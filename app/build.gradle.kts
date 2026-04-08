plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.kapt")
}

android {
    namespace = "com.example.socialnetwork"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.socialnetwork"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildFeatures {
        viewBinding = true
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}


dependencies {
    // Import Bom (Bill of Materials) để quản lý phiên bản dễ dàng hơn
    val composeBom = platform("androidx.compose:compose-bom:2024.02.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    // Các thư viện Compose cốt lõi
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.runtime:runtime") // <-- Thư viện lỗi đang báo thiếu
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")

    // Hỗ trợ Activity và ViewModel cho Compose
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")

    // Tooling để debug (chỉ chạy trên máy ảo/máy thật khi debug)
    debugImplementation("androidx.compose.ui:ui-tooling")
    // Firebase BOM để đồng bộ version
    implementation(platform("com.google.firebase:firebase-bom:34.11.0"))

    // Firebase core services
    implementation("com.google.firebase:firebase-auth")        // Authentication
    implementation("com.google.firebase:firebase-firestore")   // Cloud Firestore
    implementation("com.google.firebase:firebase-database")    // Realtime Database
    implementation("com.google.firebase:firebase-storage")  // Cloud Storage
    kapt("com.github.bumptech.glide:compiler:4.16.0")
    //implementation("com.google.firebase:firebase-auth-ktx")        // Authentication
    //implementation("com.google.firebase:firebase-firestore-ktx")   // Cloud Firestore
    //implementation("com.google.firebase:firebase-database-ktx")    // Realtime Database

    // AndroidX + UI
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.activity:activity-ktx:1.9.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // Credential API + Google Identity
    implementation("androidx.credentials:credentials:1.6.0-beta01")
    implementation("androidx.credentials:credentials-play-services-auth:1.6.0-beta01")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.2.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Test
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")

    //load ảnh
    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")

    // RecyclerView (cần cho feed)
    implementation("androidx.recyclerview:recyclerview:1.3.2")

    // Lifecycle (cho ViewModel và LiveData)
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")

    // Coroutines (cho async operations)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")
}
