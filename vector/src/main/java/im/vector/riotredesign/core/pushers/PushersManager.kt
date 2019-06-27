package im.vector.riotredesign.core.pushers

import im.vector.matrix.android.api.MatrixCallback
import im.vector.riotredesign.R
import im.vector.riotredesign.core.di.ActiveSessionHolder
import im.vector.riotredesign.core.resources.AppNameProvider
import im.vector.riotredesign.core.resources.LocaleProvider
import im.vector.riotredesign.core.resources.StringProvider
import java.util.*
import javax.inject.Inject

private const val DEFAULT_PUSHER_FILE_TAG = "mobile"

class PushersManager @Inject constructor(
        private val activeSessionHolder: ActiveSessionHolder,
        private val localeProvider: LocaleProvider,
        private val stringProvider: StringProvider,
        private val appNameProvider: AppNameProvider
) {

    fun registerPusherWithFcmKey(pushKey: String): UUID {
        val currentSession = activeSessionHolder.getActiveSession()
        var profileTag = DEFAULT_PUSHER_FILE_TAG + "_" + Math.abs(currentSession.myUserId.hashCode())

        return currentSession.addHttpPusher(
                pushKey,
                stringProvider.getString(R.string.pusher_app_id),
                profileTag,
                localeProvider.current().language,
                appNameProvider.getAppName(),
                currentSession.sessionParams.credentials.deviceId ?: "MOBILE",
                stringProvider.getString(R.string.pusher_http_url),
                false,
                true
        )
    }

    fun unregisterPusher(pushKey: String, callback: MatrixCallback<Unit>) {
        val currentSession = activeSessionHolder.getSafeActiveSession() ?: return
        currentSession.removeHttpPusher(pushKey, stringProvider.getString(R.string.pusher_app_id), callback)
    }
}