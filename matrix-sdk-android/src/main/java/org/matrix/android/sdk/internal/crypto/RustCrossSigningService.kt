/*
 * Copyright 2021 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.crypto

import androidx.lifecycle.LiveData
import kotlinx.coroutines.runBlocking
import org.matrix.android.sdk.api.auth.UserInteractiveAuthInterceptor
import org.matrix.android.sdk.api.session.crypto.crosssigning.CrossSigningService
import org.matrix.android.sdk.api.session.crypto.crosssigning.DeviceTrustResult
import org.matrix.android.sdk.api.session.crypto.crosssigning.MXCrossSigningInfo
import org.matrix.android.sdk.api.session.crypto.crosssigning.PrivateKeysInfo
import org.matrix.android.sdk.api.session.crypto.crosssigning.UserTrustResult
import org.matrix.android.sdk.api.session.crypto.crosssigning.isVerified
import org.matrix.android.sdk.api.session.crypto.model.CryptoDeviceInfo
import org.matrix.android.sdk.api.session.crypto.model.RoomEncryptionTrustLevel
import org.matrix.android.sdk.api.util.Optional
import org.matrix.android.sdk.internal.crypto.network.OutgoingRequestsProcessor
import org.matrix.rustcomponents.sdk.crypto.Request
import javax.inject.Inject

internal class RustCrossSigningService @Inject constructor(
        private val olmMachine: OlmMachine,
        private val outgoingRequestsProcessor: OutgoingRequestsProcessor,
        private val computeShieldForGroup: ComputeShieldForGroupUseCase
) : CrossSigningService {

    /**
     * Is our own identity trusted.
     */
    override suspend fun isCrossSigningVerified(): Boolean {
        return when (val identity = olmMachine.getIdentity(olmMachine.userId())) {
            is OwnUserIdentity -> identity.verified()
            else -> false
        }
    }

    override suspend fun isUserTrusted(otherUserId: String): Boolean {
        // This seems to be used only in tests.
        return checkUserTrust(otherUserId).isVerified()
    }

    /**
     * Will not force a download of the key, but will verify signatures trust chain.
     * Checks that my trusted user key has signed the other user UserKey
     */
    override suspend fun checkUserTrust(otherUserId: String): UserTrustResult {
        val identity = olmMachine.getIdentity(olmMachine.userId())

        // While UserTrustResult has many different states, they are by the callers
        // converted to a boolean value immediately, thus we don't need to support
        // all the values.
        return if (identity != null) {
            val verified = identity.verified()

            if (verified) {
                UserTrustResult.Success
            } else {
                UserTrustResult.Failure("failed to verify $otherUserId")
            }
        } else {
            UserTrustResult.CrossSigningNotConfigured(otherUserId)
        }
    }

    /**
     * Initialize cross signing for this user.
     * Users needs to enter credentials
     */
    override suspend fun initializeCrossSigning(uiaInterceptor: UserInteractiveAuthInterceptor?) {
        // ensure our keys are sent before initialising
        outgoingRequestsProcessor.processOutgoingRequests(olmMachine) {
            it is Request.KeysUpload
        }
        olmMachine.bootstrapCrossSigning(uiaInterceptor)
    }

    /**
     * Inject the private cross signing keys, likely from backup, into our store.
     *
     * This will check if the injected private cross signing keys match the public ones provided
     * by the server and if they do so
     */
    override suspend fun checkTrustFromPrivateKeys(
            masterKeyPrivateKey: String?,
            uskKeyPrivateKey: String?,
            sskPrivateKey: String?
    ): UserTrustResult {
        val export = PrivateKeysInfo(masterKeyPrivateKey, sskPrivateKey, uskKeyPrivateKey)
        return olmMachine.importCrossSigningKeys(export)
    }

    /**
     * Get the public cross signing keys for the given user.
     *
     * @param otherUserId The ID of the user for which we would like to fetch the cross signing keys.
     */
    override suspend fun getUserCrossSigningKeys(otherUserId: String): MXCrossSigningInfo? {
        return olmMachine.getIdentity(otherUserId)?.toMxCrossSigningInfo()
    }

    override fun getLiveCrossSigningKeys(userId: String): LiveData<Optional<MXCrossSigningInfo>> {
        return olmMachine.getLiveUserIdentity(userId)
    }

    /** Get our own public cross signing keys. */
    override suspend fun getMyCrossSigningKeys(): MXCrossSigningInfo? {
        return getUserCrossSigningKeys(olmMachine.userId())
    }

    /** Get our own private cross signing keys. */
    override suspend fun getCrossSigningPrivateKeys(): PrivateKeysInfo? {
        return olmMachine.exportCrossSigningKeys()
    }

    override fun getLiveCrossSigningPrivateKeys(): LiveData<Optional<PrivateKeysInfo>> {
        return olmMachine.getLivePrivateCrossSigningKeys()
    }

    /**
     * Can we sign our other devices or other users.
     *
     * Returning true means that we have the private self-signing and user-signing keys at hand.
     */
    override fun canCrossSign(): Boolean {
        val status = olmMachine.crossSigningStatus()

        return status.hasSelfSigning && status.hasUserSigning
    }

    override fun allPrivateKeysKnown(): Boolean {
        val status = olmMachine.crossSigningStatus()

        return status.hasMaster && status.hasSelfSigning && status.hasUserSigning
    }

    /** Mark a user identity as trusted and sign and upload signatures of our user-signing key to the server. */
    override suspend fun trustUser(otherUserId: String) {
        // This is only used in a test
        val userIdentity = olmMachine.getIdentity(otherUserId)
        if (userIdentity != null) {
            userIdentity.verify()
        } else {
            throw Throwable("## CrossSigning - CrossSigning is not setup for this account")
        }
    }

    /** Mark our own master key as trusted. */
    override suspend fun markMyMasterKeyAsTrusted() {
        // This doesn't seem to be used?
        trustUser(olmMachine.userId())
    }

    /**
     * Sign one of your devices and upload the signature.
     */
    override suspend fun trustDevice(deviceId: String) {
        val device = olmMachine.getDevice(olmMachine.userId(), deviceId)
        if (device != null) {
            val verified = device.verify()
            if (verified) {
                return
            } else {
                error("This device [$deviceId] is not known, or not yours")
            }
        } else {
            error("This device [$deviceId] is not known")
        }
    }

    /**
     * Check if a device is trusted.
     *
     * This will check that we have a valid trust chain from our own master key to a device, either
     * using the self-signing key for our own devices or using the user-signing key and the master
     * key of another user.
     */
    override suspend fun checkDeviceTrust(otherUserId: String, otherDeviceId: String, locallyTrusted: Boolean?): DeviceTrustResult {
        val device = olmMachine.getDevice(otherUserId, otherDeviceId)

        return if (device != null) {
            // TODO i don't quite understand the semantics here and there are no docs for
            // DeviceTrustResult, what do the different result types mean exactly,
            // do you return success only if the Device is cross signing verified?
            // what about the local trust if it isn't? why is the local trust passed as an argument here?
            DeviceTrustResult.Success(device.trustLevel())
        } else {
            DeviceTrustResult.UnknownDevice(otherDeviceId)
        }
    }

    override suspend fun onSecretMSKGossip(mskPrivateKey: String) {
        // This seems to be unused.
    }

    override suspend fun onSecretSSKGossip(sskPrivateKey: String) {
        // This as well
    }

    override suspend fun onSecretUSKGossip(uskPrivateKey: String) {
        // And
    }

    override suspend fun shieldForGroup(userIds: List<String>): RoomEncryptionTrustLevel {
        return computeShieldForGroup(olmMachine, userIds)
    }

    override suspend fun checkTrustAndAffectedRoomShields(userIds: List<String>) {
        // TODO
        // is this needed in rust?
    }

    override fun checkSelfTrust(myCrossSigningInfo: MXCrossSigningInfo?, myDevices: List<CryptoDeviceInfo>?): UserTrustResult {
        // is this needed in rust? should be moved to internal API?
        val verified = runBlocking {
            val identity = olmMachine.getIdentity(olmMachine.userId()) as? OwnUserIdentity
            identity?.verified()
        }
        return if (verified == null) {
            UserTrustResult.CrossSigningNotConfigured(olmMachine.userId())
        } else {
            UserTrustResult.Success
        }
    }

    override fun checkOtherMSKTrusted(myCrossSigningInfo: MXCrossSigningInfo?, otherInfo: MXCrossSigningInfo?): UserTrustResult {
        // is this needed in rust? should be moved to internal API?
        return UserTrustResult.Failure("Not used in rust")
    }
}
