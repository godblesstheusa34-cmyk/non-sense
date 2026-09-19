plugins { id("com.android.application") }

android {
    namespace = "com.fluidhome"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.fluidhome"
        minSdk = 31
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }
    buildFeatures { buildConfig = true }
    buildTypes { release { isMinifyEnabled = false } }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
}
