package test.itgungnir.permission

import android.Manifest
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import my.itgungnir.permission.GPermission

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        grantResult.text = when (GPermission.allGranted(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_PHONE_STATE
        )) {
            true -> "Congratulations, All permissions requested in Splash page are granted."
            else -> "Something's wrong, not all permission requested in Splash page granted."
        }

        launchCamera.setOnClickListener {
            GPermission.with(this)
                .onGranted {
                    Toast.makeText(this, "Camera permission granted", Toast.LENGTH_SHORT).show()
                }
                .request(
                    Manifest.permission.CAMERA to "相机"
                )
        }
    }
}
