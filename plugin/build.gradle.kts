
plugins {
    id("com.gradle.plugin-publish") version "2.1.1"
}

group = "io.github.shubham225"
version = "0.1.0"
description = "A Gradle plugin for summarizing Project Reactor information"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(8)
}

gradlePlugin {
    website.set("https://github.com/shubham225/reactor-summary-plugin")
    vcsUrl.set("https://github.com/shubham225/reactor-summary-plugin.git")

    plugins {
        create("reactorSummary") {
            id = "io.github.shubham225.reactor-summary"
            implementationClass = "io.github.shubham225.ReactorSummaryPlugin"
            displayName = "Reactor Summary"
            description = "Prints a Maven-style Reactor Summary (status and time per module) for multi-module Gradle builds."
            tags.set(listOf("multi-module", "build-summary", "reporting", "reactor"))
        }
    }
}

// Add a source set for the functional test suite
val functionalTestSourceSet = sourceSets.create("functionalTest") {
}

configurations["functionalTestImplementation"].extendsFrom(configurations["testImplementation"])
configurations["functionalTestRuntimeOnly"].extendsFrom(configurations["testRuntimeOnly"])

// Add a task to run the functional tests
val functionalTest by tasks.registering(Test::class) {
    testClassesDirs = functionalTestSourceSet.output.classesDirs
    classpath = functionalTestSourceSet.runtimeClasspath
    useJUnitPlatform()
}

gradlePlugin.testSourceSets.add(functionalTestSourceSet)

tasks.named<Task>("check") {
    // Run the functional tests as part of `check`
    dependsOn(functionalTest)
}

tasks.named<Test>("test") {
    // Use JUnit Jupiter for unit tests.
    useJUnitPlatform()
}

tasks.withType<Test>().configureEach {
    failOnNoDiscoveredTests = false
}

tasks.jar {
    archiveBaseName.set("reactor-summary-plugin")
}