plugins {
    alias(libs.plugins.android.application)
    //id("org.jetbrains.kotlin.android")
    //id("com.google.gms.google-services")
    // id("kotlin-kapt") // Descomente se usar Kapt para Kotlin
}

android {
    namespace = "com.example.meumusicplayer"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.meumusicplayer"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        javaCompileOptions {
            annotationProcessorOptions {
                // Sintaxe Kotlin para argumentos do Room
                arguments["room.schemaLocation"] = "$projectDir/schemas"
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
        }
    }
    compileOptions {
        // Define o código-fonte como compatível com Java 8
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    // --- ESTE BLOCO É NECESSÁRIO ---
    // Diz ao Gradle para USAR o JDK 17 para compilar o código
    /*java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(17))
        }
    }*/
    // --- FIM ---
}

dependencies {

    // Dependências Padrão
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)

    // Testes
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // Suas Dependências
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0") // Para Glide
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("androidx.media:media:1.7.0")

    // SDK ACRCloud
    implementation(files("libs/acrcloud-universal-sdk-1.3.30.jar"))

    // Room (Banco de Dados)
    val room_version = "2.6.1"
    implementation("androidx.room:room-runtime:$room_version")
    annotationProcessor("androidx.room:room-compiler:$room_version") // Para Java
    //implementation("com.google.firebase:firebase-analytics")
    //implementation(platform("com.google.firebase:firebase-bom:34.4.0"))

}