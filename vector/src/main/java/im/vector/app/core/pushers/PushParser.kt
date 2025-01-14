/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.core.pushers

import im.vector.app.core.pushers.model.PushData
import im.vector.app.core.pushers.model.PushDataFcm
import im.vector.app.core.pushers.model.PushDataUnifiedPush
import im.vector.app.core.pushers.model.toPushData
import org.matrix.android.sdk.api.extensions.tryOrNull
import org.matrix.android.sdk.api.util.MatrixJsonParser
import javax.inject.Inject

/**
 * Parse the received data from Push. Json format are different depending on the source.
 *
 * Notifications received by FCM are formatted by the matrix gateway [1]. The data send to FCM is the content
 * of the "notification" attribute of the json sent to the gateway [2][3].
 * On the other side, with UnifiedPush, the content of the message received is the content posted to the push
 * gateway endpoint [3].
 *
 * *Note*: If we want to get the same content with FCM and unifiedpush, we can do a new sygnal pusher [4].
 *
 * [1] https://github.com/matrix-org/sygnal/blob/main/sygnal/gcmpushkin.py
 * [2] https://github.com/matrix-org/sygnal/blob/main/sygnal/gcmpushkin.py#L366
 * [3] https://spec.matrix.org/latest/push-gateway-api/
 * [4] https://github.com/p1gp1g/sygnal/blob/unifiedpush/sygnal/upfcmpushkin.py (Not tested for a while)
 */
class PushParser @Inject constructor() {
    fun parsePushDataUnifiedPush(message: ByteArray): PushData? {
        return MatrixJsonParser.getMoshi().let {
            tryOrNull { it.adapter(PushDataUnifiedPush::class.java).fromJson(String(message)) }?.toPushData()
        }
    }

    fun parsePushDataFcm(message: Map<String, String?>): PushData {
        val pushDataFcm = PushDataFcm(
                eventId = message["event_id"],
                roomId = message["room_id"],
                unread = message["unread"]?.let { tryOrNull { Integer.parseInt(it) } },
        )
        return pushDataFcm.toPushData()
    }
}
