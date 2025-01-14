/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.settings.devices.v2.verification

import im.vector.app.core.di.ActiveSessionHolder
import kotlinx.coroutines.flow.firstOrNull
import org.matrix.android.sdk.api.extensions.orFalse
import org.matrix.android.sdk.flow.flow
import timber.log.Timber
import javax.inject.Inject

class CheckIfCurrentSessionCanBeVerifiedUseCase @Inject constructor(
        private val activeSessionHolder: ActiveSessionHolder,
) {

    suspend fun execute(): Boolean {
        val session = activeSessionHolder.getSafeActiveSession()
        val cryptoSessionsCount = session?.flow()
                ?.liveUserCryptoDevices(session.myUserId)
                ?.firstOrNull()
                ?.size
                ?: 0
        val hasOtherSessions = cryptoSessionsCount > 1

        val isRecoverySetup = session
                ?.sharedSecretStorageService()
                ?.isRecoverySetup()
                .orFalse()

        Timber.d("hasOtherSessions=$hasOtherSessions (otherSessionsCount=$cryptoSessionsCount), isRecoverySetup=$isRecoverySetup")
        return hasOtherSessions || isRecoverySetup
    }
}
