import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.5.31"
    application
}

group = "io.github.divinegenesis"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://m2.dv8tion.net/releases")
    maven("https://jitpack.io")
    maven("https://maven.scijava.org/content/groups/public/")
}

dependencies {
    //Kotlin
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.2-native-mt")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:1.5.2-native-mt")
    //JDA
    implementation("net.dv8tion:JDA:4.3.0_331")
    implementation("com.jagrosh:jda-utilities:3.0.5")
    implementation("com.github.MinnDevelopment:jda-reactor:1.3.0")
    //Configuration
    implementation("org.spongepowered:configurate-extra-kotlin:4.1.2")
    implementation("org.spongepowered:configurate-hocon:4.1.2")
    //Commands
    implementation("me.mattstudios.utils:matt-framework-jda:1.1.14-BETA")
    //Logging
    implementation("ch.qos.logback:logback-classic:1.2.6")
    //Classpath scanning
    implementation("io.github.classgraph:classgraph:4.8.126")
    //Injection
    implementation("com.google.inject:guice:5.0.1")
    implementation("dev.misfitlabs.kotlinguice4:kotlin-guice:1.5.0")
    //Cache
    implementation("com.github.ben-manes.caffeine:caffeine:3.0.4")
    //Database
    implementation("com.zaxxer:HikariCP:5.0.0")
    implementation("org.jetbrains.exposed", "exposed-core", "0.35.2")
    implementation("org.jetbrains.exposed", "exposed-dao", "0.35.2")
    implementation("org.jetbrains.exposed", "exposed-jdbc", "0.35.2")
    implementation("org.jetbrains.exposed", "exposed-java-time", "0.35.2")
    implementation("mysql:mysql-connector-java:8.0.26")
    implementation("com.h2database:h2:1.4.200")

}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile>() {
    kotlinOptions.jvmTarget = "11"
}

application {
    mainClass.set("io.github.divinegenesis.communicator.AppKt")
    applicationName = "Communicator"
}