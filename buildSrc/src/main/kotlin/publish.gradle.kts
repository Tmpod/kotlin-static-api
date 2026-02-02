plugins {
    `java-library`
    `maven-publish`
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = Meta.group
            artifactId = "${Meta.name}-${project.name}"
            version = Meta.version
            description = project.description

            from(components["java"])
        }
    }

    repositories {
        mavenLocal()
    }
}
