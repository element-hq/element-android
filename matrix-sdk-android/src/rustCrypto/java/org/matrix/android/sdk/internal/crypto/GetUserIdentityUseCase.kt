/*
 * Copyright (c) 2022 The Matrix.org Foundation C.I.C.
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

import com.squareup.moshi.Moshi
import kotlinx.coroutines.withContext
import org.matrix.android.sdk.api.MatrixCoroutineDispatchers
import org.matrix.android.sdk.api.session.crypto.crosssigning.DeviceTrustLevel
import org.matrix.android.sdk.internal.crypto.model.rest.RestKeyInfo
import org.matrix.android.sdk.internal.crypto.network.RequestSender
import org.matrix.android.sdk.internal.crypto.verification.VerificationRequest
import org.matrix.rustcomponents.sdk.crypto.CryptoStoreException
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Provider
import org.matrix.rustcomponents.sdk.crypto.UserIdentity as InnerUserIdentity

internal class GetUserIdentityUseCase @Inject constructor(
        private val olmMachine: Provider<OlmMachine>,
        private val requestSender: RequestSender,
        private val coroutineDispatchers: MatrixCoroutineDispatchers,
        private val moshi: Moshi,
        private val verificationRequestFactory: VerificationRequest.Factory
) {

    @Throws(CryptoStoreException::class)
    suspend operator fun invoke(userId: String): UserIdentities? {
        val innerMachine = olmMachine.get().inner()
        val identity = try {
            withContext(coroutineDispatchers.io) {
                innerMachine.getIdentity(userId, 30u)
            }
        } catch (error: CryptoStoreException) {
            Timber.w(error, "Failed to get identity for user $userId")
            return null
        }
        val adapter = moshi.adapter(RestKeyInfo::class.java)

        return when (identity) {
            is InnerUserIdentity.Other -> {
                val verified = innerMachine.isIdentityVerified(userId)
                val masterKey = adapter.fromJson(identity.masterKey)!!.toCryptoModel().apply {
                    trustLevel = DeviceTrustLevel(verified, verified)
                }
                val selfSigningKey = adapter.fromJson(identity.selfSigningKey)!!.toCryptoModel().apply {
                    trustLevel = DeviceTrustLevel(verified, verified)
                }
                UserIdentity(
                        userId = identity.userId,
                        masterKey = masterKey,
                        selfSigningKey = selfSigningKey,
                        innerMachine = innerMachine,
                        requestSender = requestSender,
                        coroutineDispatchers = coroutineDispatchers,
                        verificationRequestFactory = verificationRequestFactory
                )
            }
            is InnerUserIdentity.Own -> {
                val verified = innerMachine.isIdentityVerified(userId)

                val masterKey = adapter.fromJson(identity.masterKey)!!.toCryptoModel().apply {
                    trustLevel = DeviceTrustLevel(verified, verified)
                }
                val selfSigningKey = adapter.fromJson(identity.selfSigningKey)!!.toCryptoModel().apply {
                    trustLevel = DeviceTrustLevel(verified, verified)
                }
                val userSigningKey = adapter.fromJson(identity.userSigningKey)!!.toCryptoModel()

                OwnUserIdentity(
                        userId = identity.userId,
                        masterKey = masterKey,
                        selfSigningKey = selfSigningKey,
                        userSigningKey = userSigningKey,
                        trustsOurOwnDevice = identity.trustsOurOwnDevice,
                        innerMachine = innerMachine,
                        requestSender = requestSender,
                        coroutineDispatchers = coroutineDispatchers,
                        verificationRequestFactory = verificationRequestFactory
                )
            }
            null                             -> null
        }
    }
}
