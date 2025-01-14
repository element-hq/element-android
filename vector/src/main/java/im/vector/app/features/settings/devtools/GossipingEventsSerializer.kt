/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.settings.devtools

import im.vector.app.core.resources.DateProvider
import org.matrix.android.sdk.api.session.crypto.model.AuditTrail
import org.matrix.android.sdk.api.session.crypto.model.ForwardInfo
import org.matrix.android.sdk.api.session.crypto.model.TrailType
import org.matrix.android.sdk.api.session.crypto.model.WithheldInfo
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
                    TrailType.IncomingKeyForward -> {
                        append("from:${info.userId}|${info.deviceId} - ")
                        (trail.info as? ForwardInfo)?.let {
                            append("chainIndex: ${it.chainIndex} ")
                        }
                    }
                    else -> {
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
