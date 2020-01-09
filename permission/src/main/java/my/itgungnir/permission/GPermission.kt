package my.itgungnir.permission

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelStoreOwner

class GPermission private constructor() {

    private lateinit var context: FragmentActivity

    private lateinit var permissionUtil: GPermissionProxy

    private var showDialogAtPermissionRejection: Boolean = false
    private var grantedCallback: (() -> Unit)? = null
    private var deniedCallback: ((Boolean) -> Unit)? = null

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
    fun showDefaultDialogsAtPermissionRejection() = apply {
        showDialogAtPermissionRejection = true
    }

    fun onGranted(block: () -> Unit) = apply {
        grantedCallback = block
    }

    /**
     * lambda中的Boolean类型：是否还可以再此请求尚未赋予的权限
     * 在不同场景下取值如下：
     * - 用户拒绝了部分权限，但没有勾选"不再提醒"按钮，此时将返回true；
     * - 用户拒绝了部分权限，且全部勾选了"不再提醒"按钮，此时将返回false；
     * - 用户拒绝了部分权限，但只在部分权限上勾选了"不再提醒"按钮，还有一些权限虽然被拒绝了，但没有勾选"不再提醒"按钮，此时将返回true。
     */
    fun onDenied(block: (Boolean) -> Unit) = apply {
        deniedCallback = block
    }

    @SuppressLint("CheckResult")
    fun request(vararg permissions: Pair<String, String>) {
        permissionUtil.requestEachCombined(*(permissions.map { it.first }.toTypedArray()))
            .subscribe {
                when {
                    it.granted -> grantedCallback?.invoke()
                    !showDialogAtPermissionRejection -> deniedCallback?.invoke(it.shouldShowRequestPermissionRationale)
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
        AlertDialog.Builder(context)
            .setMessage("请允许系统获取${lackedOnes(*permissions)}权限")
            .setCancelable(false)
            .setPositiveButton("确定") { _, _ -> request(*permissions) }
            .setNegativeButton("取消") { _, _ -> deniedCallback?.invoke(true) }
            .create()
            .show()
    }

    /**
     * This method is invoked when the user didn't grant permission for all the requests,
     * and for those not granted, the user checked the "Don't show again" checkbox.
     * A dialog will pop to ask the user to navigate to system setting page to manually grant
     * permissions for this App.
     */
    private fun requestManually(vararg permissions: Pair<String, String>) {
        AlertDialog.Builder(context)
            .setMessage("由于系统无法获取${lackedOnes(*permissions)}权限，不能正常运行，请前往设置页面手动允许")
            .setCancelable(false)
            .setPositiveButton("确定") { _, _ -> toSystemConfigPage() }
            .setNegativeButton("取消") { _, _ -> deniedCallback?.invoke(false) }
            .create()
            .show()
    }

    private fun toSystemConfigPage() {
        context.startActivity(Intent().apply {
            action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
            data = Uri.parse("package:${context.packageName}")
        })
    }
}