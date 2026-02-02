plugins {
    org.jetbrains.kotlin.jvm
}

group = Meta.group
version = Meta.version

repositories {
    mavenCentral()
    mavenLocal()
}

kotlin {
    jvmToolchain(21)
}
