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

package im.vector.app.features.crypto.recover

import im.vector.app.R
import im.vector.app.core.platform.ViewModelTask
import im.vector.app.core.platform.WaitingViewData
import im.vector.app.core.resources.StringProvider
import org.matrix.android.sdk.api.auth.UserInteractiveAuthInterceptor
import org.matrix.android.sdk.api.failure.Failure
import org.matrix.android.sdk.api.failure.MatrixError
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.crypto.crosssigning.KEYBACKUP_SECRET_SSSS_NAME
import org.matrix.android.sdk.api.session.crypto.crosssigning.MASTER_KEY_SSSS_NAME
import org.matrix.android.sdk.api.session.crypto.crosssigning.SELF_SIGNING_KEY_SSSS_NAME
import org.matrix.android.sdk.api.session.crypto.crosssigning.USER_SIGNING_KEY_SSSS_NAME
import org.matrix.android.sdk.api.session.securestorage.EmptyKeySigner
import org.matrix.android.sdk.api.session.securestorage.SharedSecretStorageService
import org.matrix.android.sdk.api.session.securestorage.SsssKeyCreationInfo
import org.matrix.android.sdk.api.session.securestorage.SsssKeySpec
import org.matrix.android.sdk.internal.crypto.crosssigning.toBase64NoPadding
import org.matrix.android.sdk.internal.crypto.keysbackup.model.KeysBackupLastVersionResult
import org.matrix.android.sdk.internal.crypto.keysbackup.model.MegolmBackupCreationInfo
import org.matrix.android.sdk.internal.crypto.keysbackup.model.rest.KeysVersion
import org.matrix.android.sdk.internal.crypto.keysbackup.model.toKeysVersionResult
import org.matrix.android.sdk.internal.crypto.keysbackup.util.extractCurveKeyFromRecoveryKey
import org.matrix.android.sdk.internal.util.awaitCallback
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject

sealed class BootstrapResult {

    data class Success(val keyInfo: SsssKeyCreationInfo) : BootstrapResult()
    object SuccessCrossSigningOnly : BootstrapResult()

    abstract class Failure(val error: String?) : BootstrapResult()

    data class GenericError(val failure: Throwable) : Failure(failure.localizedMessage)
    data class InvalidPasswordError(val matrixError: MatrixError) : Failure(null)
    class FailedToCreateSSSSKey(failure: Throwable) : Failure(failure.localizedMessage)
    class FailedToSetDefaultSSSSKey(failure: Throwable) : Failure(failure.localizedMessage)
    class FailedToStorePrivateKeyInSSSS(failure: Throwable) : Failure(failure.localizedMessage)
    object MissingPrivateKey : Failure(null)
}

interface BootstrapProgressListener {
    fun onProgress(data: WaitingViewData)
}

data class Params(
        val userInteractiveAuthInterceptor: UserInteractiveAuthInterceptor,
        val progressListener: BootstrapProgressListener? = null,
        val passphrase: String?,
        val keySpec: SsssKeySpec? = null,
        val setupMode: SetupMode
)

// TODO Rename to CreateServerRecovery
class BootstrapCrossSigningTask @Inject constructor(
        private val session: Session,
        private val stringProvider: StringProvider
) : ViewModelTask<Params, BootstrapResult> {

    override suspend fun execute(params: Params): BootstrapResult {
        val crossSigningService = session.cryptoService().crossSigningService()

        Timber.d("## BootstrapCrossSigningTask: mode:${params.setupMode} Starting...")
        // Ensure cross-signing is initialized. Due to migration it is maybe not always correctly initialized

        val shouldSetCrossSigning = !crossSigningService.isCrossSigningInitialized() ||
                (params.setupMode == SetupMode.PASSPHRASE_AND_NEEDED_SECRETS_RESET && !crossSigningService.allPrivateKeysKnown()) ||
                (params.setupMode == SetupMode.HARD_RESET)
        if (shouldSetCrossSigning) {
            Timber.d("## BootstrapCrossSigningTask: Cross signing not enabled, so initialize")
            params.progressListener?.onProgress(
                    WaitingViewData(
                            stringProvider.getString(R.string.bootstrap_crosssigning_progress_initializing),
                            isIndeterminate = true
                    )
            )

            try {
                awaitCallback<Unit> {
                    crossSigningService.initializeCrossSigning(
                            params.userInteractiveAuthInterceptor,
                            it
                    )
                }
                if (params.setupMode == SetupMode.CROSS_SIGNING_ONLY) {
                    return BootstrapResult.SuccessCrossSigningOnly
                }
            } catch (failure: Throwable) {
                return handleInitializeXSigningError(failure)
            }
        } else {
            Timber.d("## BootstrapCrossSigningTask: Cross signing already setup, go to 4S setup")
            if (params.setupMode == SetupMode.CROSS_SIGNING_ONLY) {
                // not sure how this can happen??
                return handleInitializeXSigningError(IllegalArgumentException("Cross signing already setup"))
            }
        }

        val keyInfo: SsssKeyCreationInfo

        val ssssService = session.sharedSecretStorageService

        params.progressListener?.onProgress(
                WaitingViewData(
                        stringProvider.getString(R.string.bootstrap_crosssigning_progress_pbkdf2),
                        isIndeterminate = true)
        )

        Timber.d("## BootstrapCrossSigningTask: Creating 4S key with pass: ${params.passphrase != null}")
        try {
            keyInfo = params.passphrase?.let { passphrase ->
                ssssService.generateKeyWithPassphrase(
                        UUID.randomUUID().toString(),
                        "ssss_key",
                        passphrase,
                        EmptyKeySigner(),
                        null
                )
            } ?: run {
                ssssService.generateKey(
                        UUID.randomUUID().toString(),
                        params.keySpec,
                        "ssss_key",
                        EmptyKeySigner()
                )
            }
        } catch (failure: Failure) {
            Timber.e("## BootstrapCrossSigningTask: Creating 4S - Failed to generate key <${failure.localizedMessage}>")
            return BootstrapResult.FailedToCreateSSSSKey(failure)
        }

        params.progressListener?.onProgress(
                WaitingViewData(
                        stringProvider.getString(R.string.bootstrap_crosssigning_progress_default_key),
                        isIndeterminate = true)
        )

        Timber.d("## BootstrapCrossSigningTask: Creating 4S - Set default key")
        try {
            ssssService.setDefaultKey(keyInfo.keyId)
        } catch (failure: Failure) {
            // Maybe we could just ignore this error?
            Timber.e("## BootstrapCrossSigningTask: Creating 4S - Set default key error <${failure.localizedMessage}>")
            return BootstrapResult.FailedToSetDefaultSSSSKey(failure)
        }

        Timber.d("## BootstrapCrossSigningTask: Creating 4S - gathering private keys")
        val xKeys = crossSigningService.getCrossSigningPrivateKeys()
        val mskPrivateKey = xKeys?.master ?: return BootstrapResult.MissingPrivateKey
        val sskPrivateKey = xKeys.selfSigned ?: return BootstrapResult.MissingPrivateKey
        val uskPrivateKey = xKeys.user ?: return BootstrapResult.MissingPrivateKey
        Timber.d("## BootstrapCrossSigningTask: Creating 4S - gathering private keys success")

        try {
            params.progressListener?.onProgress(
                    WaitingViewData(
                            stringProvider.getString(R.string.bootstrap_crosssigning_progress_save_msk),
                            isIndeterminate = true
                    )
            )
            Timber.d("## BootstrapCrossSigningTask: Creating 4S - Storing MSK...")
            ssssService.storeSecret(
                    MASTER_KEY_SSSS_NAME,
                    mskPrivateKey,
                    listOf(SharedSecretStorageService.KeyRef(keyInfo.keyId, keyInfo.keySpec))
            )
            params.progressListener?.onProgress(
                    WaitingViewData(
                            stringProvider.getString(R.string.bootstrap_crosssigning_progress_save_usk),
                            isIndeterminate = true
                    )
            )
            Timber.d("## BootstrapCrossSigningTask: Creating 4S - Storing USK...")
            ssssService.storeSecret(
                    USER_SIGNING_KEY_SSSS_NAME,
                    uskPrivateKey,
                    listOf(SharedSecretStorageService.KeyRef(keyInfo.keyId, keyInfo.keySpec))
            )
            params.progressListener?.onProgress(
                    WaitingViewData(
                            stringProvider.getString(R.string.bootstrap_crosssigning_progress_save_ssk), isIndeterminate = true
                    )
            )
            Timber.d("## BootstrapCrossSigningTask: Creating 4S - Storing SSK...")
            ssssService.storeSecret(
                    SELF_SIGNING_KEY_SSSS_NAME,
                    sskPrivateKey,
                    listOf(SharedSecretStorageService.KeyRef(keyInfo.keyId, keyInfo.keySpec))
            )
        } catch (failure: Failure) {
            Timber.e("## BootstrapCrossSigningTask: Creating 4S - Failed to store keys <${failure.localizedMessage}>")
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
            Timber.d("## BootstrapCrossSigningTask: Creating 4S - Checking megolm backup")

            // First ensure that in sync
            var serverVersion = awaitCallback<KeysBackupLastVersionResult> {
                session.cryptoService().keysBackupService().getCurrentVersion(it)
            }.toKeysVersionResult()
            val knownMegolmSecret = session.cryptoService().keysBackupService().getKeyBackupRecoveryKeyInfo()
            val isMegolmBackupSecretKnown = knownMegolmSecret != null && knownMegolmSecret.version == serverVersion?.version
            val shouldCreateKeyBackup = serverVersion == null ||
                    (params.setupMode == SetupMode.PASSPHRASE_AND_NEEDED_SECRETS_RESET && !isMegolmBackupSecretKnown) ||
                    (params.setupMode == SetupMode.HARD_RESET)
            if (shouldCreateKeyBackup) {
                // clear all existing backups
                while (serverVersion != null) {
                    awaitCallback<Unit> {
                        session.cryptoService().keysBackupService().deleteBackup(serverVersion!!.version, it)
                    }
                    serverVersion = awaitCallback<KeysBackupLastVersionResult> {
                        session.cryptoService().keysBackupService().getCurrentVersion(it)
                    }.toKeysVersionResult()
                }

                Timber.d("## BootstrapCrossSigningTask: Creating 4S - Create megolm backup")
                val creationInfo = awaitCallback<MegolmBackupCreationInfo> {
                    session.cryptoService().keysBackupService().prepareKeysBackupVersion(null, null, it)
                }
                val version = awaitCallback<KeysVersion> {
                    session.cryptoService().keysBackupService().createKeysBackupVersion(creationInfo, it)
                }
                // Save it for gossiping
                Timber.d("## BootstrapCrossSigningTask: Creating 4S - Save megolm backup key for gossiping")
                session.cryptoService().keysBackupService().saveBackupRecoveryKey(creationInfo.recoveryKey, version = version.version)

                extractCurveKeyFromRecoveryKey(creationInfo.recoveryKey)?.toBase64NoPadding()?.let { secret ->
                    ssssService.storeSecret(
                            KEYBACKUP_SECRET_SSSS_NAME,
                            secret,
                            listOf(SharedSecretStorageService.KeyRef(keyInfo.keyId, keyInfo.keySpec))
                    )
                }
            } else {
                Timber.d("## BootstrapCrossSigningTask: Creating 4S - Existing megolm backup found")
                // ensure we store existing backup secret if we have it!
                if (isMegolmBackupSecretKnown) {
                    // check it matches
                    val isValid = awaitCallback<Boolean> {
                        session.cryptoService().keysBackupService().isValidRecoveryKeyForCurrentVersion(knownMegolmSecret!!.recoveryKey, it)
                    }
                    if (isValid) {
                        Timber.d("## BootstrapCrossSigningTask: Creating 4S - Megolm key valid and known")
                        extractCurveKeyFromRecoveryKey(knownMegolmSecret!!.recoveryKey)?.toBase64NoPadding()?.let { secret ->
                            ssssService.storeSecret(
                                    KEYBACKUP_SECRET_SSSS_NAME,
                                    secret,
                                    listOf(SharedSecretStorageService.KeyRef(keyInfo.keyId, keyInfo.keySpec))
                            )
                        }
                    } else {
                        Timber.d("## BootstrapCrossSigningTask: Creating 4S - Megolm key is unknown by this session")
                    }
                }
            }
        } catch (failure: Throwable) {
            Timber.e("## BootstrapCrossSigningTask: Failed to init keybackup")
        }

        Timber.d("## BootstrapCrossSigningTask: mode:${params.setupMode} Finished")
        return BootstrapResult.Success(keyInfo)
    }

    private fun handleInitializeXSigningError(failure: Throwable): BootstrapResult {
        if (failure is Failure.ServerError && failure.error.code == MatrixError.M_FORBIDDEN) {
            return BootstrapResult.InvalidPasswordError(failure.error)
        }
        return BootstrapResult.GenericError(failure)
    }
}
