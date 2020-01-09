package test.itgungnir.permission

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import my.itgungnir.permission.GPermission

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        /**
         * 请求权限，使用内置风格，拒绝时弹框
         */
        btn_request_default.setOnClickListener {
            GPermission.with(this)
                // 如果加了这样代码，则说明要使用GPermission内置的弹框样式来展示弹框（即系统的AlertDialog）
                // 如果不加这行代码，则表示用户不想弹框，或想要自定义弹框样式和时机
                .showDefaultDialogsAtPermissionRejection()
                // 当所有权限都被赋予后会回调这个方法
                .onGranted { toast("所有权限均已被赋予") }
                // 当有些权限没有被赋予时会回调这个方法
                .onDenied {
                    when (it) {
                        true -> toast("部分权限没有被赋予，但用户并没有勾选所有的'不再提醒'")
                        else -> toast("部分权限没有被赋予，且用户勾选了所有的'不再提醒'")
                    }
                }
                .request(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE to "文件读写",
                    Manifest.permission.READ_PHONE_STATE to "获取手机状态"
                )
        }

        /**
         * 请求权限，使用内置风格，拒绝时不弹框
         */
        btn_request_default_no_pop.setOnClickListener {
            GPermission.with(this)
                .onGranted { toast("请求到了相机权限") }
                .onDenied {
                    when (it) {
                        true -> toast("用户禁止了相机权限，但没有勾选'不再提醒'")
                        else -> toast("用户禁止了相机权限，且勾选了'不再提醒'")
                    }
                }
                .request(
                    Manifest.permission.CAMERA to "相机"
                )
        }

        /**
         * 请求权限，使用自定义风格，拒绝时弹框
         */
        btn_request_custom.setOnClickListener {
            customRequestPermissions()
        }

        /**
         * 判断指定权限是否全部被授予
         */
        btn_all_granted.setOnClickListener {
            val allPermissionsGranted = GPermission.with(this).allGranted(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.CAMERA
            )
            when (allPermissionsGranted) {
                true -> toast("所有权限均已被授予")
                else -> toast("部分权限没有被赋予")
            }
        }

        /**
         * 跳转到应用设置页
         */
        btn_system_settings.setOnClickListener {
            openSystemSettings()
        }
    }

    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

    private fun openSystemSettings() {
        startActivity(Intent().apply {
            action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
            data = Uri.parse("package:$packageName")
        })
    }

    /**
     * 请求权限，使用自定义风格，拒绝时弹框
     */
    private fun customRequestPermissions() {
        GPermission.with(this)
            .onGranted { toast("所有权限均已被赋予") }
            .onDenied {
                if (it) {
                    CustomDialog.Builder()
                        .message("弹出这个对话框说明用户拒绝了部分权限，但并没有勾选所有的'不再提醒'")
                        // 当用户点击确定按钮时，重新请求权限
                        .onConfirm { customRequestPermissions() }
                        .onCancel { toast("用户禁止了相机权限，但没有勾选'不再提醒'") }
                        .create()
                        .show(supportFragmentManager, CustomDialog::class.java.simpleName)
                } else {
                    CustomDialog.Builder()
                        .message("弹出这个对话框说明用户拒绝了部分权限，且勾选了所有的'不再提醒'")
                        // 当用户点击确定按钮时，跳转到系统的设置页面，手动设置权限
                        .onConfirm { openSystemSettings() }
                        .onCancel { toast("用户禁止了相机权限，且勾选了'不再提醒'") }
                        .create()
                        .show(supportFragmentManager, CustomDialog::class.java.simpleName)
                }
            }
            .request(
                Manifest.permission.WRITE_EXTERNAL_STORAGE to "文件读写",
                Manifest.permission.READ_PHONE_STATE to "获取手机状态"
            )
    }
}
