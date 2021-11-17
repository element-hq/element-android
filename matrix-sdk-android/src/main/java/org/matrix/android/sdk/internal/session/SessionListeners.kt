/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.matrix.android.sdk.internal.session

import org.matrix.android.sdk.api.extensions.tryOrNull
import org.matrix.android.sdk.api.session.Session
import timber.log.Timber
import javax.inject.Inject

@SessionScope
internal class SessionListeners @Inject constructor() {

    private val listeners = mutableSetOf<Session.Listener>()

    fun addListener(listener: Session.Listener) {
        synchronized(listeners) {
            listeners.add(listener)
        }
    }

    fun removeListener(listener: Session.Listener) {
        synchronized(listeners) {
            listeners.remove(listener)
        }
    }

    fun dispatch(session: Session, block: (Session, Session.Listener) -> Unit) {
        synchronized(listeners) {
            listeners.forEach {
                tryOrNull { block(session, it) }
            }
        }
    }
}

internal fun Session?.dispatchTo(sessionListeners: SessionListeners, block: (Session, Session.Listener) -> Unit) {
    if (this == null) {
        Timber.w("You don't have any attached session")
        return
    }
    sessionListeners.dispatch(this, block)
}
