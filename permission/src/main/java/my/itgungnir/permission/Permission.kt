package my.itgungnir.permission

data class Permission(
    /**
     * Name of permission.
     */
    val name: String,
    /**
     * Whether the permission is granted.
     */
    val granted: Boolean,
    /**
     * When the user denied this permission, but didn't check the "Don't ask again" checkbox,
     * then this parameter will be set TRUE, otherwise FALSE.
     */
    val shouldShowRequestPermissionRationale: Boolean
)