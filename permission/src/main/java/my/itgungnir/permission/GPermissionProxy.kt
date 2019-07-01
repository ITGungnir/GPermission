package my.itgungnir.permission

import android.os.Build
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelStoreOwner
import io.reactivex.Observable
import io.reactivex.ObservableTransformer
import io.reactivex.subjects.PublishSubject
import java.util.*

class GPermissionProxy private constructor() {

    private lateinit var fragment: GPermissionFragment

    private val trigger = Unit

    companion object {
        /**
         * ViewModelStoreOwner is a common base class for FragmentActivity and Fragment.
         * Invoke this method to initialize GPermission and GPermissionProxy object, meanwhile,
         * bind ViewModelStoreOwner to GPermission's context variable.
         */
        fun with(component: ViewModelStoreOwner) = GPermissionProxy().apply {
            fragment = when (component) {
                is FragmentActivity -> GPermissionFragment.getInstance(component.supportFragmentManager)
                is Fragment -> GPermissionFragment.getInstance(component.childFragmentManager)
                else -> throw IllegalArgumentException("GPermission requested from wrong component.")
            }
        }
    }

    /**
     * Request permissions.
     */
    fun requestEachCombined(vararg permissions: String): Observable<Permission> =
        Observable.just(trigger).compose(ensureEachCombined(*permissions))

    private fun <T> ensureEachCombined(vararg permissions: String): ObservableTransformer<T, Permission> {
        return ObservableTransformer {
            request(*permissions)
                .buffer(permissions.size)
                .flatMap { permissions ->
                    if (permissions.isEmpty()) {
                        Observable.empty()
                    } else {
                        val combineName = permissions.map { it.name }.reduce { acc, s -> "$acc, $s" }
                        val combineGranted = permissions.all { it.granted }
                        val combineRationale = permissions.any { it.shouldShowRequestPermissionRationale }
                        Observable.just(Permission(combineName, combineGranted, combineRationale))
                    }
                }
        }
    }

    private fun request(vararg permissions: String): Observable<Permission> {
        if (permissions.isEmpty()) {
            throw IllegalArgumentException("GPermissions.requestEachCombined requires at least one input permission")
        }
        return requestImplementation(*permissions)
    }

    private fun requestImplementation(vararg permissions: String): Observable<Permission> {
        val list = ArrayList<Observable<Permission>>(permissions.size)
        val unrequestedPermissions = ArrayList<String>()
        for (permission in permissions) {
            if (isGranted(permission)) {
                list.add(
                    Observable.just(
                        Permission(
                            name = permission,
                            granted = true,
                            shouldShowRequestPermissionRationale = false
                        )
                    )
                )
                continue
            }
            if (isRevoked(permission)) {
                list.add(
                    Observable.just(
                        Permission(
                            name = permission,
                            granted = false,
                            shouldShowRequestPermissionRationale = false
                        )
                    )
                )
                continue
            }
            var subject = fragment.getPermissionSubject(permission)
            if (null == subject) {
                unrequestedPermissions.add(permission)
                subject = PublishSubject.create()
                fragment.setPermissionSubject(permission, subject)
            }
            list.add(subject)
        }
        if (unrequestedPermissions.isNotEmpty()) {
            val unrequestedPermissionsArray = unrequestedPermissions.toTypedArray()
            fragment.requestPermissions(unrequestedPermissionsArray)
        }
        return Observable.concat(Observable.fromIterable(list))
    }

    private fun isGranted(permission: String) =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.M || fragment.isPermissionGranted(permission)

    private fun isRevoked(permission: String) =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.M || fragment.isPermissionRevoked(permission)
}