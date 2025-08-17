import org.jetbrains.kotlin.compose.compiler.gradle.ComposeFeatureFlag
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.TaskAction

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.lombok)
    kotlin("plugin.serialization") version libs.versions.kotlin
}

android {
    namespace = "com.project.lumina.client"
    compileSdk = 36

    // 配置原生库路径
    sourceSets {
        getByName("main") {
            jniLibs.srcDirs("libs")
        }
    }
    
    defaultConfig {
        applicationId = "com.project.lumina.client"
        minSdk = 28
        targetSdk = 36
        versionCode = 230
        versionName = "B23"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ndk {
            //noinspection ChromeOsAbiSupport
            abiFilters.addAll(setOf("arm64-v8a", "armeabi-v7a"))
        }
        externalNativeBuild {
            cmake {
                cppFlags("")
            }
        }
    }
    
    signingConfigs {
        create("shared") {
            enableV1Signing = true
            enableV2Signing = true
            enableV3Signing = true

            storeFile = rootDir.resolve("buildKey.jks")
            keyAlias = "UntrustedKey"
            storePassword = "123456"
            keyPassword = "123456"
        }
    }
    
    packaging {
        jniLibs.useLegacyPackaging = true
        resources.excludes.addAll(
            setOf(
                "DebugProbesKt.bin"
            )
        )
        resources.pickFirsts.addAll(
            setOf(
                "META-INF/INDEX.LIST",
                "META-INF/io.netty.versions.properties",
                "META-INF/DEPENDENCIES"
            )
        )
        
        // 添加ABI排除规则
        jniLibs.excludes.addAll(
            setOf(
                "**/libx86*.so",
                "**/libmips*.so"
            )
        )
    }
    
    buildTypes {
        getByName("debug") {
            isMinifyEnabled = false
            isDebuggable = true
            signingConfig = signingConfigs.getByName("shared")
        }
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            isDebuggable = false
            signingConfig = signingConfigs.getByName("shared")
        }
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    
    kotlinOptions {
        jvmTarget = "17"
    }
    
    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }
    
    composeCompiler {
        includeTraceMarkers = false
        includeSourceInformation = false
        generateFunctionKeyMetaClasses = false
        featureFlags = setOf(
            ComposeFeatureFlag.OptimizeNonSkippingGroups,
            ComposeFeatureFlag.PausableComposition
        )
    }
}

dependencies {
    // 添加对libs目录中JAR文件的支持
    implementation(fileTree(mapOf("dir" to "libs", "include" to arrayOf("*.jar"))))
    
    implementation(libs.leveldb)
    implementation(libs.ui.graphics)
    implementation(libs.androidx.compose.material)
    implementation(libs.androidx.foundation.android)
    implementation(libs.androidx.window)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    debugImplementation(platform(libs.log4j.bom))
    debugImplementation(libs.log4j.api)
    debugImplementation(libs.log4j.core)
    implementation(libs.bundles.netty)
    implementation(libs.expiringmap)
    implementation(libs.network.common)
    implementation(platform(libs.fastutil.bom))
    implementation(libs.fastutil.long.common)
    implementation(libs.fastutil.long.obj.maps)
    implementation(libs.fastutil.int.obj.maps)
    implementation(libs.fastutil.obj.int.maps)
    implementation(libs.jose4j)
    implementation(libs.math)
    implementation(libs.nbt)
    implementation(libs.snappy)
    implementation(libs.guava)
    implementation(libs.gson)
    implementation(libs.http.client)
    implementation(libs.bcprov)
    implementation(libs.okhttp)
   
    implementation("com.amplitude:analytics-android:1.+")
    implementation("com.github.SmartToolFactory:Compose-Colorful-Sliders:1.2.2")
    implementation("io.coil-kt:coil-compose:2.4.0")
    implementation("com.google.accompanist:accompanist-systemuicontroller:0.30.1")
    implementation(project(":animatedux"))
    implementation(project(":Pixie"))
    implementation(project(":Lunaris"))
    implementation(project(":TablerIcons"))
    
    implementation(libs.kotlinx.serialization.json.jvm)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.material.icons.extended)
    implementation("com.fasterxml.jackson.core:jackson-databind:2.16.1")
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation("com.google.accompanist:accompanist-drawablepainter:0.30.1")
    
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    implementation("androidx.activity:activity-compose:1.8.0")
    
}

// 添加验证任务 - 在构建时检查.so文件是否包含
abstract class CheckNativeLibsTask : DefaultTask() {
    
    @get:InputDirectory
    abstract val libsDir: DirectoryProperty
    
    @TaskAction
    fun checkLibs() {
        val libsDirFile = libsDir.get().asFile
        if (!libsDirFile.exists() || !libsDirFile.isDirectory) {
            throw GradleException("Native libs directory not found: ${libsDirFile.absolutePath}")
        }

        val v8aDir = File(libsDirFile, "arm64-v8a")
        val v7aDir = File(libsDirFile, "armeabi-v7a")
        
        val v8aFile = File(v8aDir, "libnative-lib.so")
        val v7aFile = File(v7aDir, "libnative-lib.so")
        
        if (!v8aFile.exists()) {
            throw GradleException("Missing v8a library: ${v8aFile.absolutePath}")
        }
        
        if (!v7aFile.exists()) {
            throw GradleException("Missing v7a library: ${v7aFile.absolutePath}")
        }
        
        println("Native libraries verified:")
        println("- ARM64-V8A: ${v8aFile.absolutePath} (Size: ${v8aFile.length()} bytes)")
        println("- ARMEABI-V7A: ${v7aFile.absolutePath} (Size: ${v7aFile.length()} bytes)")
    }
}

tasks.register<CheckNativeLibsTask>("checkNativeLibs") {
    libsDir.set(layout.projectDirectory.dir("libs"))
}

tasks.named("preBuild") {
    dependsOn("checkNativeLibs")
}