/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session

import org.matrix.android.sdk.api.extensions.tryOrNull
import org.matrix.android.sdk.api.session.Session
import timber.log.Timber
import java.util.concurrent.CopyOnWriteArraySet
import javax.inject.Inject

@SessionScope
internal class SessionListeners @Inject constructor() {

    private val listeners = CopyOnWriteArraySet<Session.Listener>()

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
