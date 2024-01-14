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

import im.vector.app.R
import im.vector.app.core.device.GetDeviceInfoUseCase
import im.vector.app.core.di.ActiveSessionHolder
import im.vector.app.core.resources.AppNameProvider
import im.vector.app.core.resources.LocaleProvider
import im.vector.app.core.resources.StringProvider
import im.vector.app.features.mdm.MdmData
import im.vector.app.features.mdm.MdmService
import org.matrix.android.sdk.api.session.pushers.HttpPusher
import org.matrix.android.sdk.api.session.pushers.Pusher
import java.util.UUID
import javax.inject.Inject
import kotlin.math.abs

internal const val DEFAULT_PUSHER_FILE_TAG = "mobile"

class PushersManager @Inject constructor(
        private val unifiedPushHelper: UnifiedPushHelper,
        private val activeSessionHolder: ActiveSessionHolder,
        private val localeProvider: LocaleProvider,
        private val stringProvider: StringProvider,
        private val appNameProvider: AppNameProvider,
        private val getDeviceInfoUseCase: GetDeviceInfoUseCase,
        private val mdmService: MdmService,
) {
    suspend fun testPush() {
        val currentSession = activeSessionHolder.getActiveSession()

        currentSession.pushersService().testPush(
                unifiedPushHelper.getPushGateway() ?: return,
                stringProvider.getString(R.string.pusher_app_id),
                unifiedPushHelper.getEndpointOrToken().orEmpty(),
                TEST_EVENT_ID
        )
    }

    suspend fun enqueueRegisterPusherWithFcmKey(pushKey: String): UUID {
        return enqueueRegisterPusher(
                pushKey = pushKey,
                gateway = mdmService.getData(MdmData.DefaultPushGatewayUrl, stringProvider.getString(R.string.pusher_http_url))
        )
    }

    suspend fun enqueueRegisterPusher(
            pushKey: String,
            gateway: String
    ): UUID {
        val currentSession = activeSessionHolder.getActiveSession()
        val pusher = createHttpPusher(pushKey, gateway)
        return currentSession.pushersService().enqueueAddHttpPusher(pusher)
    }

    private suspend fun createHttpPusher(
            pushKey: String,
            gateway: String
    ) = HttpPusher(
            pushkey = pushKey,
            appId = stringProvider.getString(R.string.pusher_app_id),
            profileTag = DEFAULT_PUSHER_FILE_TAG + "_" + abs(activeSessionHolder.getActiveSession().myUserId.hashCode()),
            lang = localeProvider.current().language,
            appDisplayName = appNameProvider.getAppName(),
            deviceDisplayName = getDeviceInfoUseCase.execute().displayName().orEmpty(),
            url = gateway,
            enabled = true,
            deviceId = activeSessionHolder.getActiveSession().sessionParams.deviceId,
            append = false,
            withEventIdOnly = true,
    )

    suspend fun registerEmailForPush(email: String) {
        val currentSession = activeSessionHolder.getActiveSession()
        val appName = appNameProvider.getAppName()
        currentSession.pushersService().addEmailPusher(
                email = email,
                lang = localeProvider.current().language,
                emailBranding = appName,
                appDisplayName = appName,
                deviceDisplayName = currentSession.sessionParams.deviceId
        )
    }

    fun getPusherForCurrentSession(): Pusher? {
        val session = activeSessionHolder.getSafeActiveSession() ?: return null
        val deviceId = session.sessionParams.deviceId
        return session.pushersService().getPushers().firstOrNull { it.deviceId == deviceId }
    }

    suspend fun unregisterEmailPusher(email: String) {
        val currentSession = activeSessionHolder.getSafeActiveSession() ?: return
        currentSession.pushersService().removeEmailPusher(email)
    }

    suspend fun unregisterPusher(pushKey: String) {
        val currentSession = activeSessionHolder.getSafeActiveSession() ?: return
        currentSession.pushersService().removeHttpPusher(pushKey, stringProvider.getString(R.string.pusher_app_id))
    }

    companion object {
        const val TEST_EVENT_ID = "\$THIS_IS_A_FAKE_EVENT_ID"
    }
}
