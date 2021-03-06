/*
 * Copyright 2019 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.app.core.pushers

import android.content.Context
import im.vector.app.R
import im.vector.app.core.di.ActiveSessionHolder
import im.vector.app.core.resources.AppNameProvider
import im.vector.app.core.resources.LocaleProvider
import im.vector.app.core.resources.StringProvider
import org.matrix.android.sdk.api.MatrixCallback
import org.matrix.android.sdk.api.util.Cancelable
import java.util.UUID
import javax.inject.Inject
import kotlin.math.abs

private const val DEFAULT_PUSHER_FILE_TAG = "mobile"

class PushersManager @Inject constructor(
        private val activeSessionHolder: ActiveSessionHolder,
        private val localeProvider: LocaleProvider,
        private val stringProvider: StringProvider,
        private val appNameProvider: AppNameProvider
) {
    fun testPush(context: Context, callback: MatrixCallback<Unit>): Cancelable {
        val currentSession = activeSessionHolder.getActiveSession()

        return currentSession.testPush(
                UPHelper.getPushGateway(context)!!,
                stringProvider.getString(R.string.pusher_app_id),
                UPHelper.getUpEndpoint(context)!!,
                TEST_EVENT_ID,
                callback
        )
    }

    fun registerPusherWithFcmKey(pushKey: String): UUID {
        return registerPusher(
                pushKey,
                stringProvider.getString(R.string.pusher_http_url)
        )
    }

    fun registerPusher(pushKey: String, gateway: String): UUID {
        val currentSession = activeSessionHolder.getActiveSession()
        val profileTag = DEFAULT_PUSHER_FILE_TAG + "_" + abs(currentSession.myUserId.hashCode())

        return currentSession.addHttpPusher(
                pushKey,
                stringProvider.getString(R.string.pusher_app_id),
                profileTag,
                localeProvider.current().language,
                appNameProvider.getAppName(),
                currentSession.sessionParams.deviceId ?: "MOBILE",
                gateway,
                append = false,
                withEventIdOnly = true
        )
    }

    fun unregisterPusher(context: Context, callback: MatrixCallback<Unit>) {
        val currentSession = activeSessionHolder.getSafeActiveSession() ?: return
        callback.let { currentSession.removeHttpPusher(
                UPHelper.getUpEndpoint(context)!!,
                UPHelper.getPushGateway(context)!!,
                it
        )
        }
    }

    fun unregisterPusher(pushKey: String, callback: MatrixCallback<Unit>) {
        val currentSession = activeSessionHolder.getSafeActiveSession() ?: return
        callback.let { currentSession.removeHttpPusher(pushKey, stringProvider.getString(R.string.pusher_http_url), it) }
    }

    companion object {
        const val TEST_EVENT_ID = "\$THIS_IS_A_FAKE_EVENT_ID"
    }
}
