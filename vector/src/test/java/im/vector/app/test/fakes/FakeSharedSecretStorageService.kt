/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.test.fakes

import io.mockk.every
import io.mockk.mockk
import org.matrix.android.sdk.api.listeners.ProgressListener
import org.matrix.android.sdk.api.session.securestorage.IntegrityResult
import org.matrix.android.sdk.api.session.securestorage.KeyInfoResult
import org.matrix.android.sdk.api.session.securestorage.KeyRef
import org.matrix.android.sdk.api.session.securestorage.KeySigner
import org.matrix.android.sdk.api.session.securestorage.SharedSecretStorageError
import org.matrix.android.sdk.api.session.securestorage.SharedSecretStorageService
import org.matrix.android.sdk.api.session.securestorage.SsssKeyCreationInfo
import org.matrix.android.sdk.api.session.securestorage.SsssKeySpec

class FakeSharedSecretStorageService : SharedSecretStorageService by mockk() {

    var integrityResult: IntegrityResult = IntegrityResult.Error(SharedSecretStorageError.OtherError(IllegalStateException()))
    var _defaultKey: KeyInfoResult = KeyInfoResult.Error(SharedSecretStorageError.OtherError(IllegalStateException()))

    override suspend fun generateKey(keyId: String, key: SsssKeySpec?, keyName: String, keySigner: KeySigner?): SsssKeyCreationInfo {
        TODO("Not yet implemented")
    }

    override suspend fun generateKeyWithPassphrase(
            keyId: String,
            keyName: String,
            passphrase: String,
            keySigner: KeySigner,
            progressListener: ProgressListener?
    ): SsssKeyCreationInfo {
        TODO("Not yet implemented")
    }

    override fun getKey(keyId: String): KeyInfoResult {
        TODO("Not yet implemented")
    }

    override fun getDefaultKey() = _defaultKey

    override suspend fun setDefaultKey(keyId: String) {
        TODO("Not yet implemented")
    }

    override fun hasKey(keyId: String): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun storeSecret(name: String, secretBase64: String, keys: List<KeyRef>) {
        TODO("Not yet implemented")
    }

    override fun getAlgorithmsForSecret(name: String): List<KeyInfoResult> {
        TODO("Not yet implemented")
    }

    override suspend fun getSecret(name: String, keyId: String?, secretKey: SsssKeySpec): String {
        TODO("Not yet implemented")
    }

    override fun checkShouldBeAbleToAccessSecrets(secretNames: List<String>, keyId: String?) = integrityResult

    @Deprecated("Requesting custom secrets not yet support by rust stack, prefer requestMissingSecrets")
    override suspend fun requestSecret(name: String, myOtherDeviceId: String) {
        TODO("Not yet implemented")
    }

    override suspend fun requestMissingSecrets() {
        TODO("Not yet implemented")
    }

    fun givenIsRecoverySetupReturns(isRecoverySetup: Boolean) {
        every { isRecoverySetup() } returns isRecoverySetup
    }
}
