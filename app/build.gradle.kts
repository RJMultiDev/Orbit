plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.qx.orbit.bili"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.qx.orbit.bili"
        minSdk = 30
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
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
    useLibrary("wear-sdk")
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.activity.compose)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation("androidx.compose.material3:material3")
    implementation("androidx.wear.compose:compose-navigation:1.3.1")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation(libs.compose.ui.tooling)
    implementation("androidx.compose.material:material-icons-core:1.6.0")
    implementation(libs.core.splashscreen)
    implementation(libs.play.services.wearable)
    implementation(libs.ui)
    implementation(libs.ui.graphics)
    implementation(libs.ui.tooling.preview)
    implementation(libs.wear.tooling.preview)

    // Retrofit + OkHttp + Gson
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.gson)
    implementation(libs.coroutines.core)
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.2")

    // ijkplayer + DanmakuFlameMaster + brotlij
    implementation(project(":ijkplayer-java"))
    implementation(project(":DanmakuFlameMaster"))

    // Brotli decompression
    implementation("org.brotli:dec:0.1.2")

    // Protobuf (for danmaku parsing)
    implementation("com.google.protobuf:protobuf-javalite:3.21.12")

    // Jsoup (for HTML parsing in OpusApi, CookieRefreshApi)
    implementation("org.jsoup:jsoup:1.17.2")


    // Coil for Compose
    implementation("io.coil-kt:coil-compose:2.6.0")

    // AppCompat + RecyclerView
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.recyclerview:recyclerview:1.3.2")

    // ZXing (for QR code in LoginApi)
    implementation("com.google.zxing:core:3.5.3")

    // PhotoView (for image viewer)
    implementation("com.github.chrisbanes:PhotoView:2.3.0")

    // EventBus
    implementation("org.greenrobot:eventbus:3.3.1")

    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.ui.test.junit4)
    debugImplementation(libs.ui.test.manifest)
    debugImplementation(libs.ui.tooling)
}
