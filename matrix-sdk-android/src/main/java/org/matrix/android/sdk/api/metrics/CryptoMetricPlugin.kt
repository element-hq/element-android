/*
 * Copyright 2022 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.api.metrics

import android.util.LruCache
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.session.crypto.MXCryptoError

sealed class CryptoEvent {

    data class FailedToDecryptToDevice(
            val error: String?
    ) : CryptoEvent()

    data class FailedToSendToDevice(val eventTye: String) : CryptoEvent()

    data class UnableToDecryptRoomMessage(
            val sessionId: String,
            val error: String?
    ) : CryptoEvent()

    data class LateDecryptRoomMessage(val sessionId: String, val source: String) : CryptoEvent()
}

abstract class CryptoMetricPlugin {

    internal sealed class Report {
        data class RoomE2EEReport(val error: MXCryptoError.Base, val sessionId: String) : Report()
        data class ToDeviceDecryptReport(val error: Throwable) : Report()
        data class ToDeviceSendReport(val error: Throwable) : Report()
        data class OnRoomKeyImported(val sessionId: String, val source: String) : Report()
    }

    // should I scope that to some parent job?
    val scope = CoroutineScope(SupervisorJob())

    private val channel = Channel<Report>(capacity = Channel.UNLIMITED)

    // Basic to avoid double reporting for same session and detect late reception
    private val uisiCache = LruCache<String, Unit>(200)

    init {
        scope.launch {
            for (ev in channel) {
                handleEvent(ev)
            }
        }
    }

    private fun handleEvent(ev: Report) {
        when (ev) {
            is Report.RoomE2EEReport -> {
                if (uisiCache.get(ev.sessionId) == null) {
                    uisiCache.put(ev.sessionId, Unit)
                    captureEvent(
                            CryptoEvent.UnableToDecryptRoomMessage(
                                    sessionId = ev.sessionId,
                                    error = ev.error.errorType.toString()
                            )
                    )
                }
            }
            is Report.ToDeviceDecryptReport -> {
                captureEvent(CryptoEvent.FailedToDecryptToDevice(ev.error.message.toString()))
            }
            is Report.ToDeviceSendReport -> {
                captureEvent(CryptoEvent.FailedToSendToDevice(ev.error.message.orEmpty()))
            }
            is Report.OnRoomKeyImported -> {
                if (uisiCache.get(ev.sessionId) != null) {
                    // ok we have an uisi for this session
                    captureEvent(
                            CryptoEvent.LateDecryptRoomMessage(
                                    sessionId = ev.sessionId,
                                    source = ev.source
                            )
                    )
                }
            }
        }
    }

    fun onFailedToDecryptRoomMessage(error: MXCryptoError.Base, sessionId: String) {
        channel.trySend(
                Report.RoomE2EEReport(error, sessionId)
        )
    }

    fun onFailToSendToDevice(failure: Throwable) {
        channel.trySend(
                Report.ToDeviceSendReport(failure)
        )
    }
    fun onFailToDecryptToDevice(failure: Throwable) {
        channel.trySend(
                Report.ToDeviceDecryptReport(failure)
        )
    }

    fun onRoomKeyImported(sessionId: String, source: String) {
        channel.trySend(
                Report.OnRoomKeyImported(sessionId = sessionId, source = source)
        )
    }

    protected abstract fun captureEvent(cryptoEvent: CryptoEvent)
}
