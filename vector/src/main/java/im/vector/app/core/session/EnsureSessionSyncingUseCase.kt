/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.core.session

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import im.vector.app.core.di.ActiveSessionHolder
import im.vector.app.core.extensions.startSyncing
import org.matrix.android.sdk.api.session.sync.SyncState
import timber.log.Timber
import javax.inject.Inject

class EnsureSessionSyncingUseCase @Inject constructor(
        @ApplicationContext private val context: Context,
        private val activeSessionHolder: ActiveSessionHolder,
) {
    fun execute() {
        val session = activeSessionHolder.getSafeActiveSession() ?: return
        if (session.syncService().getSyncState() == SyncState.Idle) {
            Timber.w("EnsureSessionSyncingUseCase: start syncing")
            session.startSyncing(context)
        }
    }
}
