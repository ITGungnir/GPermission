package my.itgungnir.permission

data class Permission(
    val name: String,
    val granted: Boolean,
    val shouldShowRequestPermissionRationale: Boolean
)