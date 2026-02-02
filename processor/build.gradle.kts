plugins {
    id("shared")
    id("publish")
}

dependencies {
    implementation(project(":annotations"))
    implementation(libs.bundles.processor)
}
