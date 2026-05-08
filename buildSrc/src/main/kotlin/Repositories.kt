import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.kotlin.dsl.maven

/** The PvP Legacy Maven repository. */
fun RepositoryHandler.vl(project: Project, private: Boolean = true, snapshots: Boolean = true): MavenArtifactRepository {
    val visibility = if (private) "private" else "public"
    val repo = if (snapshots) "snapshots" else "releases"
    return maven("https://maven.aoelite.me/${visibility}/${repo}") {
        name = "vl-$visibility-$repo"
        // TODO: In the future, no credentials will be needed, as authentication will support Zero Trust.
        credentials {
            username = project.findProperty("vl.maven.user") as? String? ?: System.getenv("VL_MAVEN_USER")
            password = project.findProperty("vl.maven.pass") as? String? ?: System.getenv("VL_MAVEN_PASS")
        }
    }
}
