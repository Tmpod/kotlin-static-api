plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
    implementation(libs.kotlin.gradlePlugin)
}
