/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2022 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.sync

import org.matrix.android.sdk.api.session.sync.SyncService
import org.matrix.android.sdk.internal.di.SessionId
import org.matrix.android.sdk.internal.di.WorkManagerProvider
import org.matrix.android.sdk.internal.session.SessionState
import org.matrix.android.sdk.internal.session.sync.job.SyncThread
import org.matrix.android.sdk.internal.session.sync.job.SyncWorker
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Provider

internal class DefaultSyncService @Inject constructor(
        @SessionId val sessionId: String,
        private val workManagerProvider: WorkManagerProvider,
        private val syncThreadProvider: Provider<SyncThread>,
        private val syncTokenStore: SyncTokenStore,
        private val syncRequestStateTracker: SyncRequestStateTracker,
        private val sessionState: SessionState,
) : SyncService {
    private var syncThread: SyncThread? = null

    override fun requireBackgroundSync() {
        SyncWorker.requireBackgroundSync(workManagerProvider, sessionId)
    }

    override fun startAutomaticBackgroundSync(timeOutInSeconds: Long, repeatDelayInSeconds: Long) {
        SyncWorker.automaticallyBackgroundSync(workManagerProvider, sessionId, timeOutInSeconds, repeatDelayInSeconds)
    }

    override fun stopAnyBackgroundSync() {
        SyncWorker.stopAnyBackgroundSync(workManagerProvider)
    }

    override fun startSync(fromForeground: Boolean) {
        Timber.i("Starting sync thread")
        assert(sessionState.isOpen)
        val localSyncThread = getSyncThread()
        localSyncThread.setInitialForeground(fromForeground)
        if (!localSyncThread.isAlive) {
            localSyncThread.start()
        } else {
            localSyncThread.restart()
            Timber.w("Attempt to start an already started thread")
        }
    }

    override fun stopSync() {
        assert(sessionState.isOpen)
        syncThread?.kill()
        syncThread = null
    }

    override fun getSyncStateLive() = getSyncThread().liveState()

    override fun syncFlow() = getSyncThread().syncFlow()

    override fun getSyncState() = getSyncThread().currentState()

    override fun isSyncThreadAlive() = getSyncThread().isAlive

    override fun getSyncRequestStateFlow() = syncRequestStateTracker.syncRequestState

    override fun hasAlreadySynced(): Boolean {
        return syncTokenStore.getLastToken() != null
    }

    private fun getSyncThread(): SyncThread {
        return syncThread ?: syncThreadProvider.get().also {
            syncThread = it
        }
    }
}
