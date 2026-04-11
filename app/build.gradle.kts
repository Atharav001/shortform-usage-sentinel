plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.google.services)
}

android {
    namespace = "com.example.scrollersdashboard"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.scrollersdashboard"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

// Rename the APK file and copy it to Downloads
tasks.whenTaskAdded {
    if (name.startsWith("assemble")) {
        val buildType = name.replace("assemble", "").lowercase()
        if (buildType.isNotEmpty()) {
            tasks.named(name) {
                doLast {
                    val buildDir = layout.buildDirectory.get().asFile
                    val apkDir = File(buildDir, "outputs/apk/$buildType")
                    if (apkDir.exists()) {
                        apkDir.listFiles()?.forEach { file ->
                            if (file.name.endsWith(".apk")) {
                                val newName = "ScrollersDashboard-${buildType}.apk"
                                val renamedFile = File(file.parent, newName)
                                file.renameTo(renamedFile)
                                
                                // Export to Downloads folder
                                val downloadsDir = File(System.getProperty("user.home"), "Downloads")
                                if (downloadsDir.exists()) {
                                    val destination = File(downloadsDir, newName)
                                    renamedFile.copyTo(destination, overwrite = true)
                                    println("APK exported to: ${destination.absolutePath}")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation("androidx.compose.material:material-icons-extended")
    
    // UI components for XML layouts
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.cardview)
    implementation(libs.androidx.recyclerview)

    // Biometric
    implementation("androidx.biometric:biometric:1.2.0-alpha05")
    
    // Glance for Widgets
    implementation("androidx.glance:glance-appwidget:1.1.0")
    
    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
