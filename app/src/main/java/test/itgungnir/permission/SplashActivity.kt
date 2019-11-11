package test.itgungnir.permission

import android.Manifest
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import my.itgungnir.permission.GPermission

class SplashActivity : AppCompatActivity() {

    override fun onResume() {
        super.onResume()

        GPermission.with(this)
            .showDialogAtPermissionRejection()
            .onGranted {
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
            .onDenied {
                finish()
            }
            .request(
                Manifest.permission.WRITE_EXTERNAL_STORAGE to "文件读写",
                Manifest.permission.READ_PHONE_STATE to "获取手机状态"
            )
    }
}