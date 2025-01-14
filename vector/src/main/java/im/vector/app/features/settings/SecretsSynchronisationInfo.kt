/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.settings

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import org.matrix.android.sdk.api.extensions.orFalse
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.crypto.crosssigning.KEYBACKUP_SECRET_SSSS_NAME
import org.matrix.android.sdk.api.session.crypto.crosssigning.MASTER_KEY_SSSS_NAME
import org.matrix.android.sdk.api.session.crypto.crosssigning.SELF_SIGNING_KEY_SSSS_NAME
import org.matrix.android.sdk.api.session.crypto.crosssigning.USER_SIGNING_KEY_SSSS_NAME
import org.matrix.android.sdk.flow.flow

data class SecretsSynchronisationInfo(
        val isBackupSetup: Boolean,
        val isCrossSigningEnabled: Boolean,
        val isCrossSigningTrusted: Boolean,
        val allPrivateKeysKnown: Boolean,
        val megolmBackupAvailable: Boolean,
        val megolmSecretKnown: Boolean,
        val isMegolmKeyIn4S: Boolean
)

fun Session.liveSecretSynchronisationInfo(): Flow<SecretsSynchronisationInfo> {
    val sessionFlow = flow()
    return combine(
            sessionFlow.liveUserAccountData(setOf(MASTER_KEY_SSSS_NAME, USER_SIGNING_KEY_SSSS_NAME, SELF_SIGNING_KEY_SSSS_NAME, KEYBACKUP_SECRET_SSSS_NAME)),
            sessionFlow.liveCrossSigningInfo(myUserId),
            sessionFlow.liveCrossSigningPrivateKeys()
    ) { _, crossSigningInfo, pInfo ->
        // first check if 4S is already setup
        val is4SSetup = sharedSecretStorageService().isRecoverySetup()
        val isCrossSigningEnabled = crossSigningInfo.getOrNull() != null
        val isCrossSigningTrusted = crossSigningInfo.getOrNull()?.isTrusted() == true
        val allPrivateKeysKnown = pInfo.getOrNull()?.allKnown().orFalse()

        val keysBackupService = cryptoService().keysBackupService()
        val currentBackupVersion = keysBackupService.currentBackupVersion
        val megolmBackupAvailable = currentBackupVersion != null
        val savedBackupKey = keysBackupService.getKeyBackupRecoveryKeyInfo()

        val megolmKeyKnown = savedBackupKey?.version == currentBackupVersion
        SecretsSynchronisationInfo(
                isBackupSetup = is4SSetup,
                isCrossSigningEnabled = isCrossSigningEnabled,
                isCrossSigningTrusted = isCrossSigningTrusted,
                allPrivateKeysKnown = allPrivateKeysKnown,
                megolmBackupAvailable = megolmBackupAvailable,
                megolmSecretKnown = megolmKeyKnown,
                isMegolmKeyIn4S = sharedSecretStorageService().isMegolmKeyInBackup()
        )
    }
            .distinctUntilChanged()
}
