plugins {
    org.jetbrains.kotlin.jvm
}

group = Meta.group
version = Meta.version

repositories {
    mavenCentral()
    mavenLocal()
    vl(project, private = Meta.isPrivate, snapshots = false)
    vl(project, private = Meta.isPrivate, snapshots = true)
}

kotlin {
    jvmToolchain(21)
}
