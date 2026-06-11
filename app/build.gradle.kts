import com.android.build.gradle.internal.cxx.configure.gradleLocalProperties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.devtools.ksp)
    alias(libs.plugins.kotlinSerialization)
    // Firebase 사용 시점에 google-services.json 추가 후 아래 두 플러그인을 활성화한다.
    // alias(libs.plugins.googleServices)
    // alias(libs.plugins.firebase.crashlytics)
}

android {
    namespace = "com.trueedu.tong"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.trueedu.tong"
        minSdk = 29
        targetSdk = 36
        versionCode = getVersionCodeProvider().get()
        versionName = getVersionName()

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        named("debug") {
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
        register("release") {
            val localProps = gradleLocalProperties(rootDir, providers)
            val storeFilePath = localProps.getProperty("STORE_FILE") ?: System.getenv("RELEASE_STORE_FILE")
            if (storeFilePath != null) {
                storeFile = file(storeFilePath)
                storePassword = localProps.getProperty("STORE_PASSWORD") ?: System.getenv("RELEASE_STORE_PASSWORD")
                keyAlias = localProps.getProperty("KEY_ALIAS") ?: System.getenv("RELEASE_KEY_ALIAS")
                keyPassword = localProps.getProperty("KEY_PASSWORD") ?: System.getenv("RELEASE_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        debug {
            versionNameSuffix = "-DEV"
            signingConfig = signingConfigs.getByName("debug")
        }
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
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
    kotlin {
        compilerOptions {
            jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
        }
    }
    buildFeatures {
        buildConfig = true
        compose = true
    }
    hilt {
        enableAggregatingTask = false
    }
}

fun getVersionCodeProvider(): Provider<Int> {
    return providers.exec {
        commandLine("git", "tag", "--list")
    }.standardOutput.asText.map { output ->
        val tagList = output.trim()
        if (tagList.isEmpty()) {
            1
        } else {
            tagList.split("\n").filter { it.isNotEmpty() }.size
        }
    }
}

fun getVersionName(): String {
    return try {
        val tagOutput = providers.exec {
            commandLine("git", "describe", "--tags", "--abbrev=0")
            isIgnoreExitValue = true
        }.standardOutput.asText.get()

        val tag = tagOutput.trim().removePrefix("v")
        val version = if (tag.isNotEmpty()) tag else "0.0.1"
        println("App versionName: $version")
        version
    } catch (e: Exception) {
        println("App versionName: 0.0.1 (fallback)")
        "0.0.1"
    }
}

// 버전 정보 확인을 위한 Gradle Task
tasks.register("printVersionInfo") {
    group = "versioning"
    description = "Print detailed version information"

    doLast {
        val tagListOutput = providers.exec {
            commandLine("git", "tag", "--list")
        }.standardOutput.asText.get()

        val tagList = tagListOutput.trim()
        val tagCount = if (tagList.isEmpty()) 1 else tagList.split("\n").filter { it.isNotEmpty() }.size

        val tagOutput = providers.exec {
            commandLine("git", "describe", "--tags", "--abbrev=0")
            isIgnoreExitValue = true
        }.standardOutput.asText.get()

        val tag = tagOutput.trim().removePrefix("v")
        val versionCode = getVersionCodeProvider().get()

        println("=== Version Information ===")
        println("Git tag: $tag")
        println("Tag count: $tagCount")
        println("Version code: $versionCode")
        println("Version name: ${getVersionName()}")
        println("========================")
    }
}

// KSP가 특정 환경에서 generated/ksp/<variant>/kotlin 디렉터리를 생성하지 못하는 경우가 있어,
// downstream 작업에서 NoSuchFileException이 발생할 수 있다. (빈 디렉터리라도 보장)
tasks.matching { it.name.startsWith("ksp") && it.name.endsWith("Kotlin") }.configureEach {
    doFirst {
        val variantName = name.removePrefix("ksp").removeSuffix("Kotlin")
        val variantDir = variantName.replaceFirstChar { it.lowercase() }
        layout.buildDirectory.dir("generated/ksp/$variantDir/kotlin").get().asFile.mkdirs()
        layout.buildDirectory.dir("generated/ksp/$variantDir/java").get().asFile.mkdirs()
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.activity.compose)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.material)
    implementation(libs.material.extended)
    implementation(libs.navigation.compose)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // Hilt
    implementation(libs.hilt)
    implementation(libs.hilt.navigation.compose)
    ksp(libs.bundles.ksps)
    compileOnly(libs.ksp.gradle.plugin)
    ksp(libs.symbol.processing.api)

    // Room
    implementation(libs.androidx.room.common)
    implementation(libs.androidx.room.ktx)

    // Network
    implementation(libs.retrofit)
    implementation(libs.retrofit.serialization)
    implementation(libs.kotlinx.serialization.json)
    implementation(platform(libs.okhttp.bom))
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)
    debugImplementation(libs.chucker.debug)
    releaseImplementation(libs.chucker.release)

    // Flipper
    debugImplementation(libs.flipper)
    debugImplementation(libs.flipper.network.plugin)
    debugImplementation(libs.soloader)
    releaseImplementation(libs.flipper.noop)

    // Image
    implementation(libs.coil.compose)

    // Analytics & Logging
    implementation(libs.amplitude)
    implementation(libs.timber)

    // Firebase (google-services.json 추가 후 사용)
    implementation(platform(libs.firebase.bom))
    implementation(libs.bundles.firebase)

    // Test
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
}
