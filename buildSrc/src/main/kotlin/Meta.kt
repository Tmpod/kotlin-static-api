object Meta {
    const val name = "static-api"
    const val group = "dev.tmpod"
    const val version = "0.2"

    val isSnapshot: Boolean get() = version.endsWith("-SNAPSHOT")
    /** Whether this library is currently private. Used in Maven uploads. */
    const val isPrivate = false
}
