plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "tv.danmaku.ijk.media.player"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}
