plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.protobuf)
}

import java.util.Properties
import java.io.FileInputStream

// Load signing configuration if available
val signingPropsFile = rootProject.file("signing.properties")
val signingProps = if (signingPropsFile.exists()) {
    Properties().apply { 
        load(FileInputStream(signingPropsFile))
    }
} else null

android {
    namespace = "fi.darklake.wallet"
    compileSdk = 36

    defaultConfig {
        applicationId = "fi.darklake.wallet"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        // Set deterministic build timestamp (SOURCE_DATE_EPOCH environment variable or fixed timestamp)
        val buildTimestamp = System.getenv("SOURCE_DATE_EPOCH")?.toLongOrNull()?.times(1000L) 
            ?: 1734307200000L // Fixed timestamp: 2024-12-16 00:00:00 UTC
        buildConfigField("long", "BUILD_TIMESTAMP", "${buildTimestamp}L")
    }

    // Signing configuration for reproducible builds
    signingConfigs {
        create("release") {
            if (signingProps != null) {
                storeFile = file(signingProps.getProperty("KEYSTORE_FILE"))
                storePassword = signingProps.getProperty("KEYSTORE_PASSWORD")
                keyAlias = signingProps.getProperty("KEY_ALIAS")
                keyPassword = signingProps.getProperty("KEY_PASSWORD")
                
                // Use deterministic signing algorithm
                enableV1Signing = true
                enableV2Signing = true
                enableV3Signing = true
                enableV4Signing = false // Disable for broader compatibility
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            
            // Reproducible build settings
            isDebuggable = false
            isJniDebuggable = false
            
            // Apply signing configuration if available
            if (signingProps != null) {
                signingConfig = signingConfigs.getByName("release")
            }
            
            // Set deterministic APK naming
            setProperty("archivesBaseName", "darklake-wallet-${defaultConfig.versionName}")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true // Required for buildConfigField
    }
    
    packaging {
        // Exclude files that may vary between builds
        resources {
            excludes += setOf(
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/license.txt",
                "META-INF/NOTICE",
                "META-INF/NOTICE.txt",
                "META-INF/notice.txt",
                "META-INF/ASL2.0",
                "META-INF/*.kotlin_module",
                "META-INF/versions/9/previous-compilation-data.bin",
                "META-INF/INDEX.LIST",
                "META-INF/io.netty.versions.properties"
            )
        }
        // Ensure deterministic file ordering in APK
        dex {
            useLegacyPackaging = false
        }
    }
}

// Configure protobuf compilation
protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:${libs.versions.protobuf.get()}"
    }
    plugins {
        create("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:${libs.versions.grpc.get()}"
        }
        create("grpckt") {
            artifact = "io.grpc:protoc-gen-grpc-kotlin:${libs.versions.grpcKotlin.get()}:jdk8@jar"
        }
    }
    generateProtoTasks {
        all().forEach {
            it.plugins {
                create("grpc") {
                    option("lite")
                }
                create("grpckt") {
                    option("lite")
                }
            }
            it.builtins {
                create("java") {
                    option("lite")
                }
                create("kotlin") {
                    option("lite")
                }
            }
        }
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.bitcoinj.core) {
        exclude(group = "org.bouncycastle", module = "bcprov-jdk15to18")
        exclude(group = "org.bouncycastle", module = "bcprov-jdk18on")
    }
    implementation(project(":libs:SolanaKT:solana"))
    // Bouncy Castle is provided by SolanaKT
    implementation(libs.kotlinx.serialization.json)
    // implementation(libs.seed.vault) // TODO: Add correct Seed Vault dependency
    implementation(libs.androidx.security.crypto)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.android)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.client.logging)
    implementation(libs.coil.compose)
    
    // gRPC dependencies
    implementation(libs.grpc.okhttp)
    implementation(libs.grpc.protobuf.lite)
    implementation(libs.grpc.kotlin.stub)
    implementation(libs.protobuf.kotlin.lite)
    implementation(libs.kotlinx.coroutines.android)
    
    testImplementation(libs.junit)
    testImplementation("org.mockito:mockito-core:5.8.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.2.1")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("androidx.arch.core:core-testing:2.2.0")
    testImplementation("app.cash.turbine:turbine:1.0.0")
    
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    androidTestImplementation("androidx.compose.ui:ui-test-manifest")
    
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}