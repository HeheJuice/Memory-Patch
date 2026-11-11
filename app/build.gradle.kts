plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.HeheJuice.MemeryPatch"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.HeheJuice.MemeryPatch"
        minSdk = 35
        targetSdk = 36
        versionCode = 10
        versionName = "V.1.0"
    }

    signingConfigs {
        create("stableDebug") {
            storeFile = file("debug.keystore")
            storePassword = System.getenv("RELEASE_SIGNING_PASSWORD") ?: "android"
            keyAlias = System.getenv("RELEASE_SIGNING_KEY_ALIAS") ?: "HeheJuice"
            keyPassword = System.getenv("RELEASE_SIGNING_KEY_PASSWORD") ?: "android"
            
            // Explicitly enable V1, V2, and V3 signing schemes
            enableV1Signing = true
            enableV2Signing = true
            enableV3Signing = true
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("stableDebug")
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    // ⚠️ 重要：DexKit 需要加载 native 库 (.so)
    // 必须将 useLegacyPackaging 设为 true，否则 Xposed 环境无法提取并加载该库
    packaging {
    jniLibs {
        useLegacyPackaging = true
        // 防止 Gradle 试图 strip dexkit 并报错
        keepDebugSymbols.add("**/libdexkit.so")
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
    compileOnly("de.robv.android.xposed:api:82")
    implementation("androidx.appcompat:appcompat:1.6.1")
    
    // ⚠️ 添加 DexKit 依赖
    implementation("org.luckypray:dexkit:2.0.7") 
}