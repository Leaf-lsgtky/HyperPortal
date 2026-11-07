plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.github.leaf.hyperportal"
    compileSdk = 37
    ndkVersion = "26.1.10909125"

    defaultConfig {
        applicationId = "com.github.leaf.hyperportal"
        minSdk = 34
        targetSdk = 37
        versionCode = 7
        versionName = "0.1.1"

        ndk {
            abiFilters += listOf("arm64-v8a")
        }
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    signingConfigs {
        create("release") {
            val credentials = rootProject.file(".sign/qianming.txt")
            if (!credentials.isFile) {
                throw GradleException(
                    "Missing .sign/qianming.txt. Put the release keystore and qianming.txt in .sign/ (git-ignored).",
                )
            }
            val lines = credentials.readLines().map { it.trim() }
            fun signingValue(key: String): String {
                val index = lines.indexOf(key)
                if (index < 0 || index + 1 >= lines.size || lines[index + 1].isEmpty()) {
                    throw GradleException("Missing $key in ${credentials.path}")
                }
                return lines[index + 1]
            }
            storeFile = rootProject.file(".sign/my-release-key.jks")
            storePassword = signingValue("KEYSTORE_PASSWORD")
            keyAlias = signingValue("ALIAS")
            keyPassword = signingValue("KEY_PASSWORD")
        }
    }

    buildTypes {
        release {
            // Normal development/test builds stay unminified. Pass
            // -Phyperportal.minify=true (python tools/build.py bridge --r8)
            // only for a pre-release R8 verification build. AES is never R8'd.
            isMinifyEnabled = (project.findProperty("hyperportal.minify") as String?)?.toBoolean() ?: false
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfig = signingConfigs.getByName("release")
        }
        debug {
            signingConfig = signingConfigs.getByName("release")
        }
    }

    buildFeatures {
        compose = true
    }

    packaging {
        jniLibs {
            useLegacyPackaging = false
        }
        resources {
            merges += "META-INF/xposed/*"
        }
    }
}

dependencies {
    compileOnly(libs.libxposed.api)
    implementation(libs.libxposed.service)
    implementation(libs.activity.compose)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.miuix.ui.android)
    implementation(libs.miuix.preference.android)
    implementation(libs.miuix.icons.android)
    implementation(libs.miuix.blur.android)
    implementation(libs.zxing.core)

    testImplementation(libs.junit)
}
