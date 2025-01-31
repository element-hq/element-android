/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session

import androidx.annotation.MainThread

/**
 * This defines methods associated with some lifecycle events of a session.
 */
interface SessionLifecycleObserver {
    /*
    Called when the session is opened
     */
    @MainThread
    fun onSessionStarted(session: Session) {
        // noop
    }

    /*
    Called when the session is cleared
     */
    @MainThread
    fun onClearCache(session: Session) {
        // noop
    }

    /*
    Called when the session is closed
     */
    @MainThread
    fun onSessionStopped(session: Session) {
        // noop
    }
}
