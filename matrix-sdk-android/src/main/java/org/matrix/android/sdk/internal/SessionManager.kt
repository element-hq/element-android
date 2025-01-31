/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal

import org.matrix.android.sdk.api.auth.data.SessionParams
import org.matrix.android.sdk.api.auth.data.sessionId
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.internal.auth.SessionParamsStore
import org.matrix.android.sdk.internal.di.MatrixComponent
import org.matrix.android.sdk.internal.di.MatrixScope
import org.matrix.android.sdk.internal.session.DaggerSessionComponent
import org.matrix.android.sdk.internal.session.SessionComponent
import javax.inject.Inject

@MatrixScope
internal class SessionManager @Inject constructor(
        private val matrixComponent: MatrixComponent,
        private val sessionParamsStore: SessionParamsStore
) {

    // SessionId -> SessionComponent
    private val sessionComponents = HashMap<String, SessionComponent>()

    fun getSessionComponent(sessionId: String): SessionComponent? {
        val sessionParams = sessionParamsStore.get(sessionId) ?: return null
        return getOrCreateSessionComponent(sessionParams)
    }

    fun getLastSession(): Session? {
        val sessionParams = sessionParamsStore.getLast()
        return sessionParams?.let {
            getOrCreateSession(it)
        }
    }

    fun getOrCreateSession(sessionParams: SessionParams): Session {
        return getOrCreateSessionComponent(sessionParams).session()
    }

    fun releaseSession(sessionId: String) {
        if (sessionComponents.containsKey(sessionId).not()) {
            throw RuntimeException("You don't have a session for id $sessionId")
        }
        sessionComponents.remove(sessionId)?.also {
            it.session().close()
        }
    }

    fun stopSession(sessionId: String) {
        val sessionComponent = sessionComponents[sessionId] ?: throw RuntimeException("You don't have a session for id $sessionId")
        sessionComponent.session().syncService().stopSync()
    }

    fun getOrCreateSessionComponent(sessionParams: SessionParams): SessionComponent {
        return sessionComponents.getOrPut(sessionParams.credentials.sessionId()) {
            DaggerSessionComponent
                    .factory()
                    .create(matrixComponent, sessionParams)
        }
    }
}
