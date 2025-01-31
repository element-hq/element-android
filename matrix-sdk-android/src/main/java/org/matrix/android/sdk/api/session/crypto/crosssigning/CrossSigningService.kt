/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session.crypto.crosssigning

import androidx.lifecycle.LiveData
import org.matrix.android.sdk.api.MatrixCallback
import org.matrix.android.sdk.api.auth.UserInteractiveAuthInterceptor
import org.matrix.android.sdk.api.util.Optional

interface CrossSigningService {

    fun isCrossSigningVerified(): Boolean

    fun isUserTrusted(otherUserId: String): Boolean

    /**
     * Will not force a download of the key, but will verify signatures trust chain.
     * Checks that my trusted user key has signed the other user UserKey
     */
    fun checkUserTrust(otherUserId: String): UserTrustResult

    /**
     * Initialize cross signing for this user.
     * Users needs to enter credentials
     */
    fun initializeCrossSigning(
            uiaInterceptor: UserInteractiveAuthInterceptor?,
            callback: MatrixCallback<Unit>
    )

    fun isCrossSigningInitialized(): Boolean = getMyCrossSigningKeys() != null

    fun checkTrustFromPrivateKeys(
            masterKeyPrivateKey: String?,
            uskKeyPrivateKey: String?,
            sskPrivateKey: String?
    ): UserTrustResult

    fun getUserCrossSigningKeys(otherUserId: String): MXCrossSigningInfo?

    fun getLiveCrossSigningKeys(userId: String): LiveData<Optional<MXCrossSigningInfo>>

    fun getMyCrossSigningKeys(): MXCrossSigningInfo?

    fun getCrossSigningPrivateKeys(): PrivateKeysInfo?

    fun getLiveCrossSigningPrivateKeys(): LiveData<Optional<PrivateKeysInfo>>

    fun canCrossSign(): Boolean

    fun allPrivateKeysKnown(): Boolean

    fun trustUser(
            otherUserId: String,
            callback: MatrixCallback<Unit>
    )

    fun markMyMasterKeyAsTrusted()

    /**
     * Sign one of your devices and upload the signature.
     */
    fun trustDevice(
            deviceId: String,
            callback: MatrixCallback<Unit>
    )

    fun checkDeviceTrust(
            otherUserId: String,
            otherDeviceId: String,
            locallyTrusted: Boolean?
    ): DeviceTrustResult

    // FIXME Those method do not have to be in the service
    fun onSecretMSKGossip(mskPrivateKey: String)
    fun onSecretSSKGossip(sskPrivateKey: String)
    fun onSecretUSKGossip(uskPrivateKey: String)
}
