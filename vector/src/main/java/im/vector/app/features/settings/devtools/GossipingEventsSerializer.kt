/*
 * Copyright (c) 2020 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.app.features.settings.devtools

import im.vector.app.core.resources.DateProvider
import me.gujun.android.span.span
import org.matrix.android.sdk.api.session.crypto.model.ForwardedRoomKeyContent
import org.matrix.android.sdk.api.session.crypto.model.GossipingToDeviceObject
import org.matrix.android.sdk.api.session.crypto.model.RoomKeyShareRequest
import org.matrix.android.sdk.api.session.crypto.model.SecretShareRequest
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.content.OlmEventContent
import org.matrix.android.sdk.api.session.events.model.content.SecretSendEventContent
import org.matrix.android.sdk.api.session.events.model.toModel
import org.threeten.bp.format.DateTimeFormatter

class GossipingEventsSerializer {
    private val full24DateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")

    fun serialize(eventList: List<Event>): String {
        return buildString {
            eventList.forEach {
                val clearType = it.getClearType()
                append("[${getFormattedDate(it.ageLocalTs)}] $clearType from:${it.senderId} - ")
                when (clearType) {
                    EventType.ROOM_KEY_REQUEST   -> {
                        val content = it.getClearContent().toModel<RoomKeyShareRequest>()
                        append("reqId:${content?.requestId} action:${content?.action} ")
                        if (content?.action == GossipingToDeviceObject.ACTION_SHARE_REQUEST) {
                            append("sessionId: ${content.body?.sessionId} ")
                        }
                        append("requestedBy: ${content?.requestingDeviceId}")
                    }
                    EventType.FORWARDED_ROOM_KEY -> {
                        val encryptedContent = it.content.toModel<OlmEventContent>()
                        val content = it.getClearContent().toModel<ForwardedRoomKeyContent>()

                        append("sessionId:${content?.sessionId} From Device (sender key):${encryptedContent?.senderKey}")
                        span("\nFrom Device (sender key):") {
                            textStyle = "bold"
                        }
                    }
                    EventType.ROOM_KEY           -> {
                        val content = it.getClearContent()
                        append("sessionId:${content?.get("session_id")} roomId:${content?.get("room_id")} dest:${content?.get("_dest") ?: "me"}")
                    }
                    EventType.SEND_SECRET        -> {
                        val content = it.getClearContent().toModel<SecretSendEventContent>()
                        append("requestId:${content?.requestId} From Device:${it.mxDecryptionResult?.payload?.get("sender_device")}")
                    }
                    EventType.REQUEST_SECRET     -> {
                        val content = it.getClearContent().toModel<SecretShareRequest>()
                        append("reqId:${content?.requestId} action:${content?.action} ")
                        if (content?.action == GossipingToDeviceObject.ACTION_SHARE_REQUEST) {
                            append("secretName:${content.secretName} ")
                        }
                        append("requestedBy:${content?.requestingDeviceId}")
                    }
                    EventType.ENCRYPTED          -> {
                        append("Failed to Decrypt")
                    }
                    else                         -> {
                        append("??")
                    }
                }
                append("\n")
            }
        }
    }

    private fun getFormattedDate(ageLocalTs: Long?): String {
        return ageLocalTs
                ?.let { DateProvider.toLocalDateTime(it) }
                ?.let { full24DateFormatter.format(it) }
                ?: "?"
    }
}
