import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application") version "8.7.3"
    id("org.jetbrains.kotlin.android") version "2.1.0"
    id("org.jetbrains.kotlin.plugin.compose") version "2.1.0"
}

/* Numéro de la compilation GitHub : il croît à chaque envoi, et Android
   n'accepte une mise à jour que si le numéro de version ne recule pas.
   Renommer le workflow le remettrait à zéro — ne pas le faire. */
val numeroDeCompilation = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull() ?: 1

android {
    namespace = "fr.cellule.app"
    compileSdk = 35

    /*
     * Une clé de signature fixe, versionnée avec le code.
     *
     * Sans elle, chaque machine de compilation invente sa propre clé de
     * débogage : deux APK successifs n'ont jamais la même signature, Android
     * refuse de poser l'un sur l'autre, et il faut désinstaller — ce qui
     * efface le carnet. Avec elle, chaque nouvel APK s'installe par-dessus
     * le précédent et les données restent.
     *
     * Elle ne protège rien : elle n'a pas à être secrète pour une application
     * installée à la main hors du Play Store.
     */
    signingConfigs {
        create("cellule") {
            storeFile = file("signature/cellule.jks")
            storePassword = "cellule-debug"
            keyAlias = "cellule"
            keyPassword = "cellule-debug"
        }
    }

    defaultConfig {
        applicationId = "fr.cellule.app"
        minSdk = 26
        targetSdk = 35
        versionCode = numeroDeCompilation
        versionName = "1.$numeroDeCompilation"
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("cellule")
        }
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures { compose = true }

    sourceSets["main"].java.srcDirs("src/main/kotlin")

    packaging {
        resources.excludes += setOf("/META-INF/{AL2.0,LGPL2.1}")
    }
}

kotlin {
    compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
}

dependencies {
    implementation(project(":core"))

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")

    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")

    val camerax = "1.4.1"
    implementation("androidx.camera:camera-core:$camerax")
    implementation("androidx.camera:camera-camera2:$camerax")
    implementation("androidx.camera:camera-lifecycle:$camerax")
    implementation("androidx.camera:camera-view:$camerax")
}
