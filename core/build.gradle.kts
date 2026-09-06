import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("org.jetbrains.kotlin.jvm") version "2.1.0"
}

/* Bytecode 17 : c'est ce qu'attend la chaîne Android, et ça se compile
   indifféremment avec un JDK 17 ou 21. Ce module ne connaît pas Android —
   toute la photométrie s'y teste sans appareil ni émulateur. */
kotlin {
    compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
}
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
    testLogging { events("passed", "failed") }
}
