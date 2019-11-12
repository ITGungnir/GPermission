package my.itgungnir.permission

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelStoreOwner

class GPermission private constructor() {

    private lateinit var context: FragmentActivity

    private lateinit var permissionUtil: GPermissionProxy

    private var showDialogAtPermissionRejection: Boolean = false
    private var grantedCallback: (() -> Unit)? = null
    private var deniedCallback: (() -> Unit)? = null

    companion object {
        fun with(component: ViewModelStoreOwner) = GPermission().apply {
            permissionUtil = GPermissionProxy.with(component)
            context = when (component) {
                is FragmentActivity ->
                    component
                is Fragment ->
                    component.activity ?: throw IllegalArgumentException("Fragment didn't attach to any Activity.")
                else ->
                    throw IllegalArgumentException("GPermission requested from wrong component.")
            }
        }
    }

    /**
     * If this method is invoked, dialogs will pop when not all permissions are granted by user.
     * Dialogs won't pop if this method isn't invoked.
     */
    fun showDialogAtPermissionRejection() = apply {
        showDialogAtPermissionRejection = true
    }

    fun onGranted(block: () -> Unit) = apply {
        grantedCallback = block
    }

    fun onDenied(block: () -> Unit) = apply {
        deniedCallback = block
    }

    @SuppressLint("CheckResult")
    fun request(vararg permissions: Pair<String, String>) {
        if (GPermissionDialog.isShowing) {
            return
        }
        permissionUtil.requestEachCombined(*(permissions.map { it.first }.toTypedArray()))
            .subscribe {
                when {
                    it.granted -> grantedCallback?.invoke()
                    !showDialogAtPermissionRejection -> deniedCallback?.invoke()
                    it.shouldShowRequestPermissionRationale -> requestRepeatedly(*permissions)
                    else -> requestManually(*permissions)
                }
            }
    }

    private fun granted(permission: String): Boolean =
        ContextCompat.checkSelfPermission(context.applicationContext, permission) ==
                PackageManager.PERMISSION_GRANTED

    private fun lackedOnes(vararg permissions: Pair<String, String>): List<String> {
        return permissions.filter { !granted(permission = it.first) }
            .map { it.second }
    }

    fun allGranted(vararg permissions: String) = permissions.all { granted(it) }

    /**
     * Repeatedly pop the permission-request dialogs.
     * When the user denied a permission without checking the "Don't show again" checkbox,
     * then this method will be invoked until the user finally grants all the permissions,
     * or checks all the "Don't show again" checkbox.
     */
    private fun requestRepeatedly(vararg permissions: Pair<String, String>) {
        GPermissionDialog.Builder()
            .message("请允许系统获取${lackedOnes(*permissions)}权限")
            .onConfirm { request(*permissions) }
            .onCancel { deniedCallback?.invoke() }
            .create()
            .show(context.supportFragmentManager, GPermissionDialog::class.java.name)
    }

    /**
     * This method is invoked when the user didn't grant permission for all the requests,
     * and for those not granted, the user checked the "Don't show again" checkbox.
     * A dialog will pop to ask the user to navigate to system setting page to manually grant
     * permissions for this App.
     */
    private fun requestManually(vararg permissions: Pair<String, String>) {
        GPermissionDialog.Builder()
            .message("由于系统无法获取${lackedOnes(*permissions)}权限，不能正常运行，请开启权限后再使用！")
            .onConfirm { toSystemConfigPage() }
            .onCancel { deniedCallback?.invoke() }
            .create()
            .show(context.supportFragmentManager, GPermissionDialog::class.java.name)
    }

    private fun toSystemConfigPage() {
        context.startActivity(Intent().apply {
            action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
            data = Uri.parse("package:${context.packageName}")
        })
    }
}