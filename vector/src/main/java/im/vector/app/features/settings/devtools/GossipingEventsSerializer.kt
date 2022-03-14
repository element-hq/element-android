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
import org.matrix.android.sdk.internal.crypto.model.AuditTrail
import org.matrix.android.sdk.internal.crypto.model.ForwardInfo
import org.matrix.android.sdk.internal.crypto.model.TrailType
import org.matrix.android.sdk.internal.crypto.model.WithheldInfo
import org.threeten.bp.format.DateTimeFormatter

class GossipingEventsSerializer {
    private val full24DateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")

    fun serialize(eventList: List<AuditTrail>): String {
        return buildString {
            eventList.forEach { trail ->
                val type = trail.type
                val info = trail.info
                append("[${getFormattedDate(trail.ageLocalTs)}] ${type.name} ")
                append("sessionId: ${info.sessionId} ")
                when (type) {
                    TrailType.IncomingKeyRequest -> {
                        append("from:${info.userId}|${info.deviceId} - ")
                    }
                    TrailType.OutgoingKeyForward -> {
                        append("to:${info.userId}|${info.deviceId} - ")
                        (trail.info as? ForwardInfo)?.let {
                            append("chainIndex: ${it.chainIndex} ")
                        }
                    }
                    TrailType.OutgoingKeyWithheld -> {
                        append("to:${info.userId}|${info.deviceId} - ")
                        (trail.info as? WithheldInfo)?.let {
                            append("code: ${it.code} ")
                        }
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
