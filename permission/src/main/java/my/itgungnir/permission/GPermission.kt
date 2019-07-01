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

    fun onGranted(block: () -> Unit): GPermission {
        this.grantedCallback = block
        return this
    }

    fun onDenied(block: () -> Unit): GPermission {
        this.deniedCallback = block
        return this
    }

    @SuppressLint("CheckResult")
    fun request(vararg permissions: Pair<String, String>) {
        permissionUtil.requestEachCombined(*(permissions.map { it.first }.toTypedArray()))
            .subscribe {
                when {
                    it.granted -> grantedCallback?.invoke()
                    it.shouldShowRequestPermissionRationale -> requestRepeatedly(*permissions)
                    else -> requestManually(*permissions)
                }
            }
    }

    fun allGranted(vararg permissions: String) = permissions.all { granted(it) }

    private fun lackedOnes(vararg permissions: Pair<String, String>): List<String> {
        return permissions.filter { !granted(permission = it.first) }
            .map { it.second }
    }

    private fun granted(permission: String): Boolean =
        ContextCompat.checkSelfPermission(context.applicationContext, permission) == PackageManager.PERMISSION_GRANTED

    /**
     * Repeatedly pop the permission-request dialogs.
     * When the user denied a permission without checking the "Don't show again" checkbox,
     * then this method will be invoked until the user finally grants all the permissions,
     * or checks all the "Don't show again" checkbox.
     */
    private fun requestRepeatedly(vararg permissions: Pair<String, String>) {
        // If we don't invoke commitAllowingStateLoss() method, exceptions will occur noted:
        // Can not perform this action after onSaveInstanceState
        context.supportFragmentManager.beginTransaction()
            .add(GPermissionDialog.Builder()
                .message("请允许系统获取${lackedOnes(*permissions)}权限")
                .onConfirm { request(*permissions) }
                .onCancel { deniedCallback?.invoke() }
                .create(), GPermissionDialog::class.java.name)
            .commitAllowingStateLoss()
    }

    /**
     * This method is invoked when the user didn't grant permission for all the requests,
     * and for those not granted, the user checked the "Don't show again" checkbox.
     * A dialog will pop to ask the user to navigate to system setting page to manually grant
     * permissions for this App.
     */
    private fun requestManually(vararg permissions: Pair<String, String>) {
        context.supportFragmentManager.beginTransaction()
            .add(GPermissionDialog.Builder()
                .message("由于系统无法获取${lackedOnes(*permissions)}权限，不能正常运行，请开启权限后再使用！")
                .onConfirm { toSystemConfigPage() }
                .onCancel { deniedCallback?.invoke() }
                .create(), GPermissionDialog::class.java.name)
            .commitAllowingStateLoss()
    }

    private fun toSystemConfigPage() {
        context.startActivity(Intent().apply {
            action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
            data = Uri.parse("package:${context.packageName}")
        })
    }
}