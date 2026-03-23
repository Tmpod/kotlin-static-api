plugins {
    id("shared")
    id("publish")
}

dependencies {
    implementation(project(":annotations"))
    implementation(libs.bundles.processor)

    testImplementation(libs.bundles.test)
    testRuntimeOnly(libs.bundles.test.runtime)
}

tasks.test {
    useJUnitPlatform()
}
