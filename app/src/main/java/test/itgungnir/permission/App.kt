package test.itgungnir.permission

import android.app.Application
import com.squareup.leakcanary.LeakCanary

/**
 * Description:
 *
 * Created by ITGungnir on 2019-11-12
 */
class App : Application() {

    override fun onCreate() {
        super.onCreate()
        if (LeakCanary.isInAnalyzerProcess(this)) {
            return
        }
        LeakCanary.install(this)
    }
}