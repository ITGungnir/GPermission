package my.itgungnir.permission

import android.annotation.TargetApi
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import io.reactivex.subjects.PublishSubject

class GPermissionFragment private constructor() : Fragment() {

    private val subjects = mutableMapOf<String, PublishSubject<Permission>>()

    companion object {
        const val PERMISSION_REQUEST_CODE = 0x1F

        fun getInstance(manager: FragmentManager): GPermissionFragment =
            (manager.findFragmentByTag(GPermissionFragment::class.java.name) ?: GPermissionFragment().apply {
                manager.beginTransaction().add(this, GPermissionFragment::class.java.name).commitNow()
            }) as GPermissionFragment
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (PERMISSION_REQUEST_CODE != requestCode) {
            return
        }
        for (i in 0 until permissions.size) {
            val subject = subjects[permissions[i]] ?: return
            // 将已授权或已禁用的权限删除
            subjects.remove(permissions[i])
            val granted = grantResults[i] == PackageManager.PERMISSION_GRANTED
            subject.onNext(
                Permission(
                    name = permissions[i],
                    granted = granted,
                    shouldShowRequestPermissionRationale = shouldShowRequestPermissionRationale(permissions[i])
                )
            )
            subject.onComplete()
        }
    }

    fun requestPermissions(permissions: Array<String>) =
        requestPermissions(permissions, PERMISSION_REQUEST_CODE)

    fun getPermissionSubject(permission: String) =
        subjects[permission]

    fun setPermissionSubject(permission: String, subject: PublishSubject<Permission>) {
        subjects[permission] = subject
    }

    fun isPermissionSubjectExist(permission: String) =
        subjects.containsKey(permission)

    @TargetApi(Build.VERSION_CODES.M)
    fun isPermissionGranted(permission: String): Boolean =
        activity?.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED

    @TargetApi(Build.VERSION_CODES.M)
    fun isPermissionRevoked(permission: String): Boolean =
        activity?.packageManager?.isPermissionRevokedByPolicy(permission, activity!!.packageName) ?: false
}