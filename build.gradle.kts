import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.jlleitschuh.gradle.ktlint") version "11.3.1"
    id("com.github.ben-manes.versions") version "0.46.0"
}

buildscript {
    repositories {
        google()
        gradlePluginPortal()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:8.0.0")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.8.10")
        classpath("com.google.gms:google-services:4.3.15")
        classpath("com.google.firebase:firebase-appdistribution-gradle:4.0.0")
        classpath("de.mannodermaus.gradle.plugins:android-junit5:1.8.2.1")
        classpath("com.github.triplet.gradle:play-publisher:3.8.3")
        classpath("com.google.dagger:hilt-android-gradle-plugin:2.45")
    }
}

allprojects {
    apply(plugin = "org.jlleitschuh.gradle.ktlint")

    tasks.withType<KotlinCompile>().configureEach {
        kotlinOptions {
            jvmTarget = "11"
        }
    }
}

gradle.projectsEvaluated {
    project(":app").tasks.matching { it.name.startsWith("publish") }.configureEach {
        mustRunAfter(project(":wear").tasks.matching { it.name.startsWith("publish") })
    }
}

tasks.register("clean").configure {
    delete("build")
}

ktlint {
    android.set(true)
}
