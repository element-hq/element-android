package im.vector.riotredesign.core.resources

import android.content.Context
import timber.log.Timber
import javax.inject.Inject


class AppNameProvider @Inject constructor(private val context: Context) {

    fun getAppName(): String {
        try {
            val appPackageName = context.applicationContext.packageName
            val pm = context.packageManager
            val appInfo = pm.getApplicationInfo(appPackageName, 0)
            var appName = pm.getApplicationLabel(appInfo).toString()

            // Use appPackageName instead of appName if appName contains any non-ASCII character
            if (!appName.matches("\\A\\p{ASCII}*\\z".toRegex())) {
                appName = appPackageName
            }
            return appName
        } catch (e: Exception) {
            Timber.e(e, "## AppNameProvider() : failed " + e.message)
            return "RiotXAndroid"
        }
    }
}