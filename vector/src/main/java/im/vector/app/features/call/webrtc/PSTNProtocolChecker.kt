/*
 * Copyright (c) 2021 New Vector Ltd
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

package im.vector.app.features.call.webrtc

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.extensions.tryOrNull
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.room.model.thirdparty.ThirdPartyProtocol
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

private const val PSTN_VECTOR_KEY = "im.vector.protocol.pstn"
private const val PSTN_MATRIX_KEY = "m.protocol.pstn"

@Singleton
class PSTNProtocolChecker @Inject constructor() {

    private var alreadyChecked = AtomicBoolean(false)

    private val pstnSupportListeners = emptyList<WebRtcCallManager.PSTNSupportListener>().toMutableList()
    fun addPstnSupportListener(listener: WebRtcCallManager.PSTNSupportListener) {
        pstnSupportListeners.add(listener)
    }

    fun removePstnSupportListener(listener: WebRtcCallManager.PSTNSupportListener) {
        pstnSupportListeners.remove(listener)
    }

    var supportedPSTNProtocol: String? = null
        private set

    fun checkForPSTNSupportIfNeeded(currentSession: Session?) {
        if (alreadyChecked.get()) return
        GlobalScope.checkForPSTNSupport(currentSession)
    }

    private fun CoroutineScope.checkForPSTNSupport(currentSession: Session?) = launch {
        try {
            supportedPSTNProtocol = currentSession?.getSupportedPSTN(3)
            alreadyChecked.set(true)
            if (supportedPSTNProtocol != null) {
                pstnSupportListeners.forEach {
                    tryOrNull { it.onPSTNSupportUpdated() }
                }
            }
        } catch (failure: Throwable) {
            Timber.v("Fail to get supported PSTN, will check again next time.")
        }
    }
}

suspend fun Session.getSupportedPSTN(maxTries: Int): String? {
    val thirdPartyProtocols: Map<String, ThirdPartyProtocol> = try {
        thirdPartyService().getThirdPartyProtocols()
    } catch (failure: Throwable) {
        if (maxTries == 1) {
            throw failure
        } else {
            // Wait for 10s before trying again
            delay(10_000L)
            return getSupportedPSTN(maxTries - 1)
        }
    }
    return when {
        thirdPartyProtocols.containsKey(PSTN_VECTOR_KEY) -> PSTN_VECTOR_KEY
        thirdPartyProtocols.containsKey(PSTN_MATRIX_KEY) -> PSTN_MATRIX_KEY
        else                                             -> null
    }
}
