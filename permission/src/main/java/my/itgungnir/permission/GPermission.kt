package my.itgungnir.permission

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.tbruyelle.rxpermissions2.RxPermissions

class GPermission private constructor() {

    private lateinit var context: FragmentActivity

    private lateinit var permissionUtil: RxPermissions

    private var grantedCallback: (() -> Unit)? = null
    private var deniedCallback: (() -> Unit)? = null

    companion object {
        fun with(activity: FragmentActivity) = GPermission().apply {
            context = activity
            permissionUtil = RxPermissions(activity)
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

    private fun lackOnes(vararg permissions: Pair<String, String>): List<String> {
        return permissions.filter { granted(permission = it.first) }
            .map { it.second }
    }

    private fun granted(permission: String): Boolean =
        ContextCompat.checkSelfPermission(context.applicationContext, permission) == PackageManager.PERMISSION_DENIED

    private fun requestRepeatedly(vararg permissions: Pair<String, String>) {
        GPermissionDialog.Builder()
            .message("请允许系统获取${lackOnes(*permissions)}权限")
            .onConfirm { request(*permissions) }
            .onCancel { deniedCallback?.invoke() }
            .create()
            .show(context.supportFragmentManager, GPermissionDialog::class.java.name)
    }

    private fun requestManually(vararg permissions: Pair<String, String>) {
        GPermissionDialog.Builder()
            .message("由于系统无法获取${lackOnes(*permissions)}权限，不能正常运行，请开启权限后再使用！")
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