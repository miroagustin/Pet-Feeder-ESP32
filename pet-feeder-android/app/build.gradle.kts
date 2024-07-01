plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "soa.L6.pet_feeder"
    compileSdk = 34
    defaultConfig {
        applicationId = "soa.L6.pet_feeder"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation(libs.lifecycle.livedata.ktx)
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    implementation(libs.firebase.inappmessaging)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation("org.eclipse.paho:org.eclipse.paho.client.mqttv3:1.2.5")
    //implementation("org.eclipse.paho:org.eclipse.paho.android.service:1.1.1")
    implementation ("androidx.legacy:legacy-support-v4:1.0.0")
    implementation ("com.github.hannesa2:paho.mqtt.android:4.2.4")

    implementation ("androidx.room:room-runtime:2.3.0")
    implementation ("com.jakewharton.timber:timber:5.0.1")

// Add below to fix Runtime error(ClassNotFoundException)
    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.5.2")
    implementation("androidx.localbroadcastmanager:localbroadcastmanager:1.1.0")

}