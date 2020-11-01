plugins {
    kotlin("jvm") version "1.4.10"
    application
}

group = "com.careem"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test-junit"))
}

application {
    mainClassName = "com.careem.gradle.dependencies.DependencyTreeTldr"
}