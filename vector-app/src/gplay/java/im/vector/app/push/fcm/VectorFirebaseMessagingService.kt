/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.push.fcm

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.core.di.ActiveSessionHolder
import im.vector.app.core.pushers.FcmHelper
import im.vector.app.core.pushers.PushParser
import im.vector.app.core.pushers.PushersManager
import im.vector.app.core.pushers.UnifiedPushHelper
import im.vector.app.core.pushers.VectorPushHandler
import im.vector.app.features.mdm.MdmData
import im.vector.app.features.mdm.MdmService
import im.vector.app.features.settings.VectorPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.logger.LoggerTag
import timber.log.Timber
import javax.inject.Inject

private val loggerTag = LoggerTag("Push", LoggerTag.SYNC)

@AndroidEntryPoint
class VectorFirebaseMessagingService : FirebaseMessagingService() {
    @Inject lateinit var fcmHelper: FcmHelper
    @Inject lateinit var vectorPreferences: VectorPreferences
    @Inject lateinit var activeSessionHolder: ActiveSessionHolder
    @Inject lateinit var pushersManager: PushersManager
    @Inject lateinit var pushParser: PushParser
    @Inject lateinit var vectorPushHandler: VectorPushHandler
    @Inject lateinit var unifiedPushHelper: UnifiedPushHelper
    @Inject lateinit var mdmService: MdmService

    private val scope = CoroutineScope(SupervisorJob())

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    override fun onNewToken(token: String) {
        Timber.tag(loggerTag.value).d("New Firebase token")
        fcmHelper.storeFcmToken(token)
        if (
                vectorPreferences.areNotificationEnabledForDevice() &&
                activeSessionHolder.hasActiveSession() &&
                unifiedPushHelper.isEmbeddedDistributor()
        ) {
            scope.launch {
                pushersManager.enqueueRegisterPusher(
                        pushKey = token,
                        gateway = mdmService.getData(
                                mdmData = MdmData.DefaultPushGatewayUrl,
                                defaultValue = getString(im.vector.app.config.R.string.pusher_http_url),
                        ),
                )
            }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        Timber.tag(loggerTag.value).d("New Firebase message")
        pushParser.parsePushDataFcm(message.data).let {
            vectorPushHandler.handle(it)
        }
    }
}
