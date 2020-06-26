/*
 * Copyright (c) 2020 New Vector Ltd
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

package im.vector.riotx.features.crypto.recover

import im.vector.matrix.android.api.auth.data.LoginFlowTypes
import im.vector.matrix.android.api.failure.Failure
import im.vector.matrix.android.api.failure.MatrixError
import im.vector.matrix.android.api.failure.toRegistrationFlowResponse
import im.vector.matrix.android.api.session.Session
import im.vector.matrix.android.api.session.crypto.crosssigning.KEYBACKUP_SECRET_SSSS_NAME
import im.vector.matrix.android.api.session.crypto.crosssigning.MASTER_KEY_SSSS_NAME
import im.vector.matrix.android.api.session.crypto.crosssigning.SELF_SIGNING_KEY_SSSS_NAME
import im.vector.matrix.android.api.session.crypto.crosssigning.USER_SIGNING_KEY_SSSS_NAME
import im.vector.matrix.android.api.session.securestorage.EmptyKeySigner
import im.vector.matrix.android.api.session.securestorage.SharedSecretStorageService
import im.vector.matrix.android.api.session.securestorage.SsssKeyCreationInfo
import im.vector.matrix.android.api.session.securestorage.SsssKeySpec
import im.vector.matrix.android.internal.crypto.crosssigning.toBase64NoPadding
import im.vector.matrix.android.internal.crypto.keysbackup.model.MegolmBackupCreationInfo
import im.vector.matrix.android.internal.crypto.keysbackup.model.rest.KeysVersion
import im.vector.matrix.android.internal.crypto.keysbackup.util.extractCurveKeyFromRecoveryKey
import im.vector.matrix.android.internal.crypto.model.rest.UserPasswordAuth
import im.vector.matrix.android.internal.util.awaitCallback
import im.vector.riotx.R
import im.vector.riotx.core.platform.ViewModelTask
import im.vector.riotx.core.platform.WaitingViewData
import im.vector.riotx.core.resources.StringProvider
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject

sealed class BootstrapResult {

    data class Success(val keyInfo: SsssKeyCreationInfo) : BootstrapResult()

    abstract class Failure(val error: String?) : BootstrapResult()

    class UnsupportedAuthFlow : Failure(null)

    data class GenericError(val failure: Throwable) : Failure(failure.localizedMessage)
    data class InvalidPasswordError(val matrixError: MatrixError) : Failure(null)
    class FailedToCreateSSSSKey(failure: Throwable) : Failure(failure.localizedMessage)
    class FailedToSetDefaultSSSSKey(failure: Throwable) : Failure(failure.localizedMessage)
    class FailedToStorePrivateKeyInSSSS(failure: Throwable) : Failure(failure.localizedMessage)
    object MissingPrivateKey : Failure(null)

    data class PasswordAuthFlowMissing(val sessionId: String) : Failure(null)
}

interface BootstrapProgressListener {
    fun onProgress(data: WaitingViewData)
}

data class Params(
        val userPasswordAuth: UserPasswordAuth? = null,
        val progressListener: BootstrapProgressListener? = null,
        val passphrase: String?,
        val keySpec: SsssKeySpec? = null
)

// TODO Rename to CreateServerRecovery
class BootstrapCrossSigningTask @Inject constructor(
        private val session: Session,
        private val stringProvider: StringProvider
) : ViewModelTask<Params, BootstrapResult> {

    override suspend fun execute(params: Params): BootstrapResult {
        val crossSigningService = session.cryptoService().crossSigningService()

        // Ensure cross-signing is initialized. Due to migration it is maybe not always correctly initialized
        if (!crossSigningService.isCrossSigningInitialized()) {
            params.progressListener?.onProgress(
                    WaitingViewData(
                            stringProvider.getString(R.string.bootstrap_crosssigning_progress_initializing),
                            isIndeterminate = true
                    )
            )

            try {
                awaitCallback<Unit> {
                    crossSigningService.initializeCrossSigning(params.userPasswordAuth, it)
                }
            } catch (failure: Throwable) {
                return handleInitializeXSigningError(failure)
            }
        }

        val keyInfo: SsssKeyCreationInfo

        val ssssService = session.sharedSecretStorageService

        params.progressListener?.onProgress(
                WaitingViewData(
                        stringProvider.getString(R.string.bootstrap_crosssigning_progress_pbkdf2),
                        isIndeterminate = true)
        )
        try {
            keyInfo = awaitCallback {
                params.passphrase?.let { passphrase ->
                    ssssService.generateKeyWithPassphrase(
                            UUID.randomUUID().toString(),
                            "ssss_key",
                            passphrase,
                            EmptyKeySigner(),
                            null,
                            it
                    )
                } ?: kotlin.run {
                    ssssService.generateKey(
                            UUID.randomUUID().toString(),
                            params.keySpec,
                            "ssss_key",
                            EmptyKeySigner(),
                            it
                    )
                }
            }
        } catch (failure: Failure) {
            return BootstrapResult.FailedToCreateSSSSKey(failure)
        }

        params.progressListener?.onProgress(
                WaitingViewData(
                        stringProvider.getString(R.string.bootstrap_crosssigning_progress_default_key),
                        isIndeterminate = true)
        )
        try {
            awaitCallback<Unit> {
                ssssService.setDefaultKey(keyInfo.keyId, it)
            }
        } catch (failure: Failure) {
            // Maybe we could just ignore this error?
            return BootstrapResult.FailedToSetDefaultSSSSKey(failure)
        }

        val xKeys = crossSigningService.getCrossSigningPrivateKeys()
        val mskPrivateKey = xKeys?.master ?: return BootstrapResult.MissingPrivateKey
        val sskPrivateKey = xKeys.selfSigned ?: return BootstrapResult.MissingPrivateKey
        val uskPrivateKey = xKeys.user ?: return BootstrapResult.MissingPrivateKey

        try {
            params.progressListener?.onProgress(
                    WaitingViewData(
                            stringProvider.getString(R.string.bootstrap_crosssigning_progress_save_msk),
                            isIndeterminate = true
                    )
            )
            awaitCallback<Unit> {
                ssssService.storeSecret(
                        MASTER_KEY_SSSS_NAME,
                        mskPrivateKey,
                        listOf(SharedSecretStorageService.KeyRef(keyInfo.keyId, keyInfo.keySpec)), it
                )
            }
            params.progressListener?.onProgress(
                    WaitingViewData(
                            stringProvider.getString(R.string.bootstrap_crosssigning_progress_save_usk),
                            isIndeterminate = true
                    )
            )
            awaitCallback<Unit> {
                ssssService.storeSecret(
                        USER_SIGNING_KEY_SSSS_NAME,
                        uskPrivateKey,
                        listOf(SharedSecretStorageService.KeyRef(keyInfo.keyId, keyInfo.keySpec)),
                        it
                )
            }
            params.progressListener?.onProgress(
                    WaitingViewData(
                            stringProvider.getString(R.string.bootstrap_crosssigning_progress_save_ssk), isIndeterminate = true
                    )
            )
            awaitCallback<Unit> {
                ssssService.storeSecret(
                        SELF_SIGNING_KEY_SSSS_NAME,
                        sskPrivateKey,
                        listOf(SharedSecretStorageService.KeyRef(keyInfo.keyId, keyInfo.keySpec)), it
                )
            }
        } catch (failure: Failure) {
            // Maybe we could just ignore this error?
            return BootstrapResult.FailedToStorePrivateKeyInSSSS(failure)
        }

        params.progressListener?.onProgress(
                WaitingViewData(
                        stringProvider.getString(R.string.bootstrap_crosssigning_progress_key_backup),
                        isIndeterminate = true
                )
        )
        try {
            if (session.cryptoService().keysBackupService().keysBackupVersion == null) {
                val creationInfo = awaitCallback<MegolmBackupCreationInfo> {
                    session.cryptoService().keysBackupService().prepareKeysBackupVersion(null, null, it)
                }
                val version = awaitCallback<KeysVersion> {
                    session.cryptoService().keysBackupService().createKeysBackupVersion(creationInfo, it)
                }
                // Save it for gossiping
                session.cryptoService().keysBackupService().saveBackupRecoveryKey(creationInfo.recoveryKey, version = version.version)

                awaitCallback<Unit> {
                    extractCurveKeyFromRecoveryKey(creationInfo.recoveryKey)?.toBase64NoPadding()?.let { secret ->
                        ssssService.storeSecret(
                                KEYBACKUP_SECRET_SSSS_NAME,
                                secret,
                                listOf(SharedSecretStorageService.KeyRef(keyInfo.keyId, keyInfo.keySpec)), it
                        )
                    }
                }
            }
        } catch (failure: Throwable) {
            Timber.e("## BootstrapCrossSigningTask: Failed to init keybackup")
        }

        return BootstrapResult.Success(keyInfo)
    }

    private fun handleInitializeXSigningError(failure: Throwable): BootstrapResult {
        if (failure is Failure.ServerError && failure.error.code == MatrixError.M_FORBIDDEN) {
            return BootstrapResult.InvalidPasswordError(failure.error)
        } else {
            val registrationFlowResponse = failure.toRegistrationFlowResponse()
            if (registrationFlowResponse != null) {
                return if (registrationFlowResponse.flows.orEmpty().any { it.stages?.contains(LoginFlowTypes.PASSWORD) == true }) {
                    BootstrapResult.PasswordAuthFlowMissing(registrationFlowResponse.session ?: "")
                } else {
                    // can't do this from here
                    BootstrapResult.UnsupportedAuthFlow()
                }
            }
        }
        return BootstrapResult.GenericError(failure)
    }
}
