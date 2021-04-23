/*
 * Copyright (c) 2021 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.api.session.call

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.extensions.tryOrNull
import org.matrix.android.sdk.api.session.room.model.thirdparty.ThirdPartyProtocol
import org.matrix.android.sdk.internal.session.SessionScope
import org.matrix.android.sdk.internal.session.thirdparty.GetThirdPartyProtocolsTask
import org.matrix.android.sdk.internal.task.TaskExecutor
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

private const val PSTN_VECTOR_KEY = "im.vector.protocol.pstn"
private const val PSTN_MATRIX_KEY = "m.protocol.pstn"

/**
 * This class is responsible for checking if the HS support the PSTN protocol.
 * As long as the request succeed, it'll check only once by session.
 */
@SessionScope
class PSTNProtocolChecker @Inject internal constructor(private val taskExecutor: TaskExecutor,
                                                       private val getThirdPartyProtocolsTask: GetThirdPartyProtocolsTask) {

    interface Listener {
        fun onPSTNSupportUpdated()
    }

    private var alreadyChecked = AtomicBoolean(false)

    private val pstnSupportListeners = mutableListOf<Listener>()

    fun addListener(listener: Listener) {
        pstnSupportListeners.add(listener)
    }

    fun removeListener(listener: Listener) {
        pstnSupportListeners.remove(listener)
    }

    var supportedPSTNProtocol: String? = null
        private set

    fun checkForPSTNSupportIfNeeded() {
        if (alreadyChecked.get()) return
        taskExecutor.executorScope.checkForPSTNSupport()
    }

    private fun CoroutineScope.checkForPSTNSupport() = launch {
        try {
            supportedPSTNProtocol = getSupportedPSTN(3)
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

    private suspend fun getSupportedPSTN(maxTries: Int): String? {
        val thirdPartyProtocols: Map<String, ThirdPartyProtocol> = try {
            getThirdPartyProtocolsTask.execute(Unit)
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
}
