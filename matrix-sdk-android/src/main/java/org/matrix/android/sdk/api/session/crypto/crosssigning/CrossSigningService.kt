/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.api.session.crypto.crosssigning

import androidx.lifecycle.LiveData
import org.matrix.android.sdk.api.auth.UserInteractiveAuthInterceptor
import org.matrix.android.sdk.api.session.crypto.model.CryptoDeviceInfo
import org.matrix.android.sdk.api.session.crypto.model.RoomEncryptionTrustLevel
import org.matrix.android.sdk.api.util.Optional

interface CrossSigningService {
    /**
     * Is our published identity trusted.
     */
    suspend fun isCrossSigningVerified(): Boolean

    // TODO this isn't used anywhere besides in tests?
    //  Is this the local trust concept that we have for devices?
    suspend fun isUserTrusted(otherUserId: String): Boolean

    /**
     * Will not force a download of the key, but will verify signatures trust chain.
     * Checks that my trusted user key has signed the other user UserKey
     */
    suspend fun checkUserTrust(otherUserId: String): UserTrustResult

    /**
     * Initialize cross signing for this user.
     * Users needs to enter credentials
     */
    suspend fun initializeCrossSigning(uiaInterceptor: UserInteractiveAuthInterceptor?)

    /**
     * Does our own user have a valid cross signing identity uploaded.
     *
     * In other words has any of our devices uploaded public cross signing keys to the server.
     */
    suspend fun isCrossSigningInitialized(): Boolean = getMyCrossSigningKeys() != null

    /**
     * Inject the private cross signing keys, likely from backup, into our store.
     *
     * This will check if the injected private cross signing keys match the public ones provided
     * by the server and if they do so
     */
    suspend fun checkTrustFromPrivateKeys(masterKeyPrivateKey: String?,
                                          uskKeyPrivateKey: String?,
                                          sskPrivateKey: String?): UserTrustResult

    /**
     * Get the public cross signing keys for the given user.
     *
     * @param otherUserId The ID of the user for which we would like to fetch the cross signing keys.
     */
    suspend fun getUserCrossSigningKeys(otherUserId: String): MXCrossSigningInfo?

    fun getLiveCrossSigningKeys(userId: String): LiveData<Optional<MXCrossSigningInfo>>

    /** Get our own public cross signing keys. */
    suspend fun getMyCrossSigningKeys(): MXCrossSigningInfo?

    /** Get our own private cross signing keys. */
    suspend fun getCrossSigningPrivateKeys(): PrivateKeysInfo?

    fun getLiveCrossSigningPrivateKeys(): LiveData<Optional<PrivateKeysInfo>>

    /**
     * Can we sign our other devices or other users?
     *
     * Returning true means that we have the private self-signing and user-signing keys at hand.
     */
    fun canCrossSign(): Boolean

    /** Do we have all our private cross signing keys in storage? */
    fun allPrivateKeysKnown(): Boolean

    /** Mark a user identity as trusted and sign and upload signatures of our user-signing key to the server. */
    suspend fun trustUser(otherUserId: String)

    /** Mark our own master key as trusted. */
    suspend fun markMyMasterKeyAsTrusted()

    /**
     * Sign one of your devices and upload the signature.
     */
    @Throws
    suspend fun trustDevice(deviceId: String)

    suspend fun shieldForGroup(userIds: List<String>): RoomEncryptionTrustLevel

    /**
     * Check if a device is trusted
     *
     * This will check that we have a valid trust chain from our own master key to a device, either
     * using the self-signing key for our own devices or using the user-signing key and the master
     * key of another user.
     */
    suspend fun checkDeviceTrust(otherUserId: String,
                                 otherDeviceId: String,
            // TODO what is locallyTrusted used for?
                                 locallyTrusted: Boolean?): DeviceTrustResult

    // FIXME Those method do not have to be in the service
    // TODO those three methods doesn't seem to be used anywhere?
    suspend fun onSecretMSKGossip(mskPrivateKey: String)
    suspend fun onSecretSSKGossip(sskPrivateKey: String)
    suspend fun onSecretUSKGossip(uskPrivateKey: String)
    suspend fun checkTrustAndAffectedRoomShields(userIds: List<String>)
    fun checkSelfTrust(myCrossSigningInfo: MXCrossSigningInfo?, myDevices: List<CryptoDeviceInfo>?): UserTrustResult
    fun checkOtherMSKTrusted(myCrossSigningInfo: MXCrossSigningInfo?, otherInfo: MXCrossSigningInfo?): UserTrustResult
}
