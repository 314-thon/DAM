import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt")
}

val localProps = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        file.inputStream().use { load(it) }
    }
}

val requiredFirebaseProps = listOf("FIREBASE_API_KEY", "FIREBASE_APP_ID")
val missingRequiredFirebaseProps = requiredFirebaseProps.filter {
    localProps.getProperty(it).isNullOrBlank()
}
val missingOptionalFirebaseProps = listOf(
    "FIREBASE_DB_URL",
    "FIREBASE_PROJECT_ID",
    "GOOGLE_WEB_CLIENT_ID"
).filter { localProps.getProperty(it).isNullOrBlank() }

if (missingOptionalFirebaseProps.isNotEmpty() && missingRequiredFirebaseProps.isEmpty()) {
    logger.warn(
        "AVISO: Faltan propiedades de Firebase en local.properties " +
            "(${missingOptionalFirebaseProps.joinToString(", ")}). " +
            "Revisa local.properties.example y README.md (seccion G)."
    )
}

gradle.taskGraph.whenReady {
    val requiresFirebaseConfig = allTasks.any { task ->
        task.project == project && (
            task.name.startsWith("assemble") ||
                task.name.startsWith("install") ||
                task.name.startsWith("bundle") ||
                task.name.startsWith("package") ||
                task.name.startsWith("compile") ||
                (task.name.startsWith("generate") && task.name.endsWith("BuildConfig"))
            )
    }
    if (requiresFirebaseConfig && missingRequiredFirebaseProps.isNotEmpty()) {
        throw GradleException(
            "Faltan claves obligatorias de Firebase en local.properties: " +
                "${missingRequiredFirebaseProps.joinToString(", ")}. " +
                "Agrega las credenciales al final de local.properties (sin borrar sdk.dir) " +
                "siguiendo local.properties.example y README.md (seccion G)."
        )
    }
}

android {
    namespace = "com.ifts18.unadecisionmas"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.ifts18.unadecisionmas"
        minSdk = 24
        targetSdk = 34
        versionCode = 2
        versionName = "2.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        buildConfigField(
            "String",
            "FIREBASE_DB_URL",
            "\"${localProps.getProperty("FIREBASE_DB_URL", "https://juego-limpio-ifts18-default-rtdb.firebaseio.com")}\""
        )
        buildConfigField(
            "String",
            "FIREBASE_PROJECT_ID",
            "\"${localProps.getProperty("FIREBASE_PROJECT_ID", "juego-limpio-ifts18")}\""
        )
        buildConfigField(
            "String",
            "FIREBASE_API_KEY",
            "\"${localProps.getProperty("FIREBASE_API_KEY", "")}\""
        )
        buildConfigField(
            "String",
            "FIREBASE_APP_ID",
            "\"${localProps.getProperty("FIREBASE_APP_ID", "")}\""
        )
        buildConfigField(
            "String",
            "GOOGLE_WEB_CLIENT_ID",
            "\"${localProps.getProperty("GOOGLE_WEB_CLIENT_ID", "")}\""
        )
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.10"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")

    // Jetpack Compose BoM
    implementation(platform("androidx.compose:compose-bom:2024.02.02"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    // Navigation Compose
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // Herramientas de depuracion y preview
    implementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // Lifecycle y ViewModel para Compose (MVVM)
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")

    // Lottie para Jetpack Compose
    implementation("com.airbnb.android:lottie-compose:6.4.0")

    // Base de datos local: Room y SQLite
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    kapt("androidx.room:room-compiler:$roomVersion")

    // Ranking en linea y autenticacion: Firebase (Auth + Realtime Database + Cloud Firestore)
    implementation(platform("com.google.firebase:firebase-bom:32.7.4"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-database-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")

    // Google Sign-In via Google Play Services (compatible con Android 7..16 sin depender de CredentialSelector)
    implementation("com.google.android.gms:play-services-auth:21.2.0")

    // Generacion y escaneo de codigos QR (Easter Egg SIXSEVEN)
    implementation("com.google.zxing:core:3.5.3")
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")

    testImplementation("junit:junit:4.13.2")
}
