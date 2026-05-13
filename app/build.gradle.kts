plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlinx.serialization)

    alias(libs.plugins.gms) apply false
    alias(libs.plugins.crashlytics) apply false
}

val hasGoogleServices = file("google-services.json").exists()
val gitHash = runCatching { execute("git", "rev-parse", "HEAD").take(7) }.getOrDefault("unknown")
val gitCount = runCatching { execute("git", "rev-list", "--count", "HEAD").toInt() }.getOrDefault(0)
val version = "3.0.$gitCount"

android {
    namespace = "dev.brahmkshatriya.echo"
    compileSdk = 36

    defaultConfig {
        applicationId = "dev.brahmkshatriya.echo"
        minSdk = 24
        targetSdk = 36
        versionCode = if (gitCount > 0) gitCount else 1
        versionName = "v${version}_$gitHash($gitCount)"

        ndk {
            abiFilters.addAll(listOf("armeabi-v7a", "arm64-v8a"))
        }

        resConfigs("en", "hi", "zh")
    }

    signingConfigs {
        create("release") {
            val storeFilePath = project.findProperty("signing.store.file")
                ?: project.findProperty("android.injected.signing.store.file")

            storeFilePath?.let {
                storeFile = file(it.toString())
                storePassword = (project.findProperty("signing.store.password")
                    ?: project.findProperty("android.injected.signing.store.password"))?.toString()
                keyAlias = (project.findProperty("signing.key.alias")
                    ?: project.findProperty("android.injected.signing.key.alias"))?.toString()
                keyPassword = (project.findProperty("signing.key.password")
                    ?: project.findProperty("android.injected.signing.key.password"))?.toString()
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
            )
            signingConfig = signingConfigs.getByName("release")
        }
        create("nightly") {
            initWith(getByName("release"))
            resValue("string", "app_name", "Eko Nightly")
            signingConfig = signingConfigs.getByName("release")
        }
        create("stable") {
            initWith(getByName("release"))
            signingConfig = signingConfigs.getByName("release")
        }
    }

    buildFeatures {
        buildConfig = true
        viewBinding = true
    }

    androidResources {
        @Suppress("UnstableApiUsage")
        generateLocaleConfig = true
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.google.firebase:firebase-database-ktx")
    implementation(project(":common"))
    implementation(libs.bundles.androidx)
    implementation(libs.material)
    implementation(libs.bundles.paging)
    implementation(libs.filekache)
    implementation(libs.bundles.room)
    ksp(libs.room.compiler)
    implementation(libs.bundles.koin)
    implementation(libs.bundles.media3)
    implementation(libs.bundles.coil)
    implementation("io.coil-kt.coil3:coil-core:3.3.0")

    implementation(libs.pikolo)
    implementation(libs.fadingedgelayout)
    implementation(libs.fastscroll)
    implementation(libs.kenburnsview)
    implementation(libs.nestedscrollwebview)
    implementation(libs.acsbendi.webview)
    implementation(libs.google.webrtc)
    implementation(libs.tensorflow.lite)
    implementation(libs.tensorflow.lite.support)

    if (!hasGoogleServices) return@dependencies
    implementation(libs.bundles.firebase)
    implementation(libs.okhttp)
}

if (hasGoogleServices) {
    apply(plugin = libs.plugins.gms.get().pluginId)
    apply(plugin = libs.plugins.crashlytics.get().pluginId)
}

fun execute(vararg command: String): String = providers.exec {
    commandLine(*command)
}.standardOutput.asText.get().trim()