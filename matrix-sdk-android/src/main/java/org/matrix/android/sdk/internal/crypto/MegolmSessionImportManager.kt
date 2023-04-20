/*
 * Copyright 2021 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.crypto

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.MatrixCoroutineDispatchers
import org.matrix.android.sdk.api.extensions.tryOrNull
import org.matrix.android.sdk.api.session.crypto.NewSessionListener
import org.matrix.android.sdk.api.session.crypto.model.ImportRoomKeysResult
import org.matrix.android.sdk.internal.session.SessionScope
import javax.inject.Inject

/**
 * Helper that allows listeners to be notified when a new megolm session
 * has been added to the crypto layer (could be via room keys or forward keys via sync
 * or after importing keys from key backup or manual import).
 * Can be used to refresh display when the keys are received after the message
 */
@SessionScope
internal class MegolmSessionImportManager @Inject constructor(
        private val coroutineDispatchers: MatrixCoroutineDispatchers,
        private val cryptoCoroutineScope: CoroutineScope
) {

    private val newSessionsListeners = mutableListOf<NewSessionListener>()

    fun addListener(listener: NewSessionListener) {
        synchronized(newSessionsListeners) {
            if (!newSessionsListeners.contains(listener)) {
                newSessionsListeners.add(listener)
            }
        }
    }

    fun removeListener(listener: NewSessionListener) {
        synchronized(newSessionsListeners) {
            newSessionsListeners.remove(listener)
        }
    }

    fun dispatchNewSession(roomId: String?, sessionId: String) {
        val copy = synchronized(newSessionsListeners) {
            newSessionsListeners.toList()
        }
        cryptoCoroutineScope.launch(coroutineDispatchers.computation) {
            copy.forEach {
                tryOrNull("Failed to dispatch new session import") {
                    it.onNewSession(roomId, sessionId)
                }
            }
        }
    }

    fun dispatchKeyImportResults(result: ImportRoomKeysResult) {
        result.importedSessionInfo.forEach { (roomId, senderToSessionIdMap) ->
            senderToSessionIdMap.values.forEach { sessionList ->
                sessionList.forEach { sessionId ->
                    dispatchNewSession(roomId, sessionId)
                }
            }
        }
    }
}
