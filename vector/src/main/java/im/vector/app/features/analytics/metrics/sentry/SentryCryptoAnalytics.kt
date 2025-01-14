/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.analytics.metrics.sentry

import io.sentry.Sentry
import io.sentry.SentryEvent
import io.sentry.protocol.Message
import org.matrix.android.sdk.api.metrics.CryptoEvent
import org.matrix.android.sdk.api.metrics.CryptoMetricPlugin
import javax.inject.Inject

class SentryCryptoAnalytics @Inject constructor() : CryptoMetricPlugin() {

    override fun captureEvent(cryptoEvent: CryptoEvent) {
        if (!Sentry.isEnabled()) return
        val event = SentryEvent()
        event.setTag("e2eFlavor", "rustCrypto")
        event.setTag("e2eType", "crypto")
        when (cryptoEvent) {
            is CryptoEvent.FailedToDecryptToDevice -> {
                event.message = Message().apply { message = "FailedToDecryptToDevice" }
                event.setExtra("e2eOlmError", cryptoEvent.error ?: "Unknown")
            }
            is CryptoEvent.FailedToSendToDevice -> {
                event.message = Message().apply { message = "FailedToSendToDevice" }
                event.setExtra("e2eEventType", cryptoEvent.eventTye)
            }
            is CryptoEvent.LateDecryptRoomMessage -> {
                event.message = Message().apply { message = "LateDecryptRoomMessage" }
                event.setTag("e2eSource", cryptoEvent.source)
                event.setExtra("e2eSessionId", cryptoEvent.sessionId)
            }
            is CryptoEvent.UnableToDecryptRoomMessage -> {
                event.message = Message().apply { message = "UnableToDecryptRoomMessage" }
                event.setExtra("e2eSessionId", cryptoEvent.sessionId)
                event.setTag("e2eMegolmError", cryptoEvent.error.orEmpty())
            }
        }
        Sentry.captureEvent(event)
    }
}
