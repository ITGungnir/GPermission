package test.itgungnir.permission

import android.Manifest
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import my.itgungnir.permission.GPermission

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        GPermission.with(this)
            .onGranted {
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
            .onDenied { finish() }
            .request(
                Manifest.permission.WRITE_EXTERNAL_STORAGE to "文件读写",
                Manifest.permission.READ_PHONE_STATE to "获取手机状态"
            )
    }
}