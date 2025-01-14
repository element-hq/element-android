/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package im.vector.app.features.crypto.keysbackup.settings

import com.airbnb.epoxy.TypedEpoxyController
import com.airbnb.mvrx.Async
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.Uninitialized
import im.vector.app.R
import im.vector.app.core.epoxy.errorWithRetryItem
import im.vector.app.core.epoxy.loadingItem
import im.vector.app.core.resources.StringProvider
import im.vector.app.core.ui.list.ItemStyle
import im.vector.app.core.ui.list.genericItem
import im.vector.app.features.settings.VectorPreferences
import im.vector.lib.core.utils.epoxy.charsequence.toEpoxyCharSequence
import im.vector.lib.strings.CommonPlurals
import im.vector.lib.strings.CommonStrings
import org.matrix.android.sdk.api.extensions.orFalse
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.crypto.keysbackup.KeysBackupState
import org.matrix.android.sdk.api.session.crypto.keysbackup.KeysBackupVersionTrust
import org.matrix.android.sdk.api.session.crypto.keysbackup.KeysBackupVersionTrustSignature
import java.util.UUID
import javax.inject.Inject

class KeysBackupSettingsRecyclerViewController @Inject constructor(
        private val stringProvider: StringProvider,
        private val vectorPreferences: VectorPreferences,
        private val session: Session
) : TypedEpoxyController<KeysBackupSettingViewState>() {

    var listener: Listener? = null

    override fun buildModels(data: KeysBackupSettingViewState?) {
        if (data == null) {
            return
        }

        val host = this
        var isBackupAlreadySetup = false

        val keyBackupState = data.keysBackupState
        val keyVersionResult = data.keysBackupVersion

        when (keyBackupState) {
            KeysBackupState.Unknown -> {
                errorWithRetryItem {
                    id("summary")
                    text(host.stringProvider.getString(CommonStrings.keys_backup_unable_to_get_keys_backup_data))
                    listener { host.listener?.loadKeysBackupState() }
                }

                // Nothing else to display
                return
            }
            KeysBackupState.CheckingBackUpOnHomeserver -> {
                loadingItem {
                    id("summary")
                    loadingText(host.stringProvider.getString(CommonStrings.keys_backup_settings_checking_backup_state))
                }
            }
            KeysBackupState.Disabled -> {
                genericItem {
                    id("summary")
                    title(host.stringProvider.getString(CommonStrings.keys_backup_settings_status_not_setup).toEpoxyCharSequence())
                    style(ItemStyle.BIG_TEXT)

                    if (data.keysBackupVersionTrust()?.usable == false) {
                        description(host.stringProvider.getString(CommonStrings.keys_backup_settings_untrusted_backup).toEpoxyCharSequence())
                    }
                }

                isBackupAlreadySetup = false
            }
            KeysBackupState.WrongBackUpVersion,
            KeysBackupState.NotTrusted,
            KeysBackupState.Enabling -> {
                genericItem {
                    id("summary")
                    title(host.stringProvider.getString(CommonStrings.keys_backup_settings_status_ko).toEpoxyCharSequence())
                    style(ItemStyle.BIG_TEXT)
                    if (data.keysBackupVersionTrust()?.usable == false) {
                        description(host.stringProvider.getString(CommonStrings.keys_backup_settings_untrusted_backup).toEpoxyCharSequence())
                    } else {
                        description(keyBackupState.toString().toEpoxyCharSequence())
                    }
                    endIconResourceId(R.drawable.unit_test_ko)
                }

                isBackupAlreadySetup = true
            }
            KeysBackupState.ReadyToBackUp -> {
                genericItem {
                    id("summary")
                    title(host.stringProvider.getString(CommonStrings.keys_backup_settings_status_ok).toEpoxyCharSequence())
                    style(ItemStyle.BIG_TEXT)
                    if (data.keysBackupVersionTrust()?.usable == false) {
                        description(host.stringProvider.getString(CommonStrings.keys_backup_settings_untrusted_backup).toEpoxyCharSequence())
                    } else {
                        description(host.stringProvider.getString(CommonStrings.keys_backup_info_keys_all_backup_up).toEpoxyCharSequence())
                    }
                    endIconResourceId(R.drawable.unit_test_ok)
                }

                isBackupAlreadySetup = true
            }
            KeysBackupState.WillBackUp,
            KeysBackupState.BackingUp -> {
                genericItem {
                    id("summary")
                    title(host.stringProvider.getString(CommonStrings.keys_backup_settings_status_ok).toEpoxyCharSequence())
                    style(ItemStyle.BIG_TEXT)
                    hasIndeterminateProcess(true)

                    val remainingKeysToBackup = data.remainingKeysToBackup

                    if (data.keysBackupVersionTrust()?.usable == false) {
                        description(host.stringProvider.getString(CommonStrings.keys_backup_settings_untrusted_backup).toEpoxyCharSequence())
                    } else {
                        description(
                                host.stringProvider
                                        .getQuantityString(CommonPlurals.keys_backup_info_keys_backing_up, remainingKeysToBackup, remainingKeysToBackup)
                                        .toEpoxyCharSequence()
                        )
                    }
                }

                isBackupAlreadySetup = true
            }
            null -> Unit
        }

        if (isBackupAlreadySetup) {
            // Add infos
            genericItem {
                id("version")
                title(host.stringProvider.getString(CommonStrings.keys_backup_info_title_version).toEpoxyCharSequence())
                description(keyVersionResult?.version.orEmpty().toEpoxyCharSequence())
            }

            genericItem {
                id("algorithm")
                title(host.stringProvider.getString(CommonStrings.keys_backup_info_title_algorithm).toEpoxyCharSequence())
                description(keyVersionResult?.algorithm.orEmpty().toEpoxyCharSequence())
            }

            if (vectorPreferences.developerMode()) {
                buildKeysBackupTrust(data.keysBackupVersionTrust)
            }
        }

        // Footer
        keysBackupSettingFooterItem {
            id("footer")

            if (isBackupAlreadySetup) {
                textButton1(host.stringProvider.getString(CommonStrings.keys_backup_settings_restore_backup_button))
                clickOnButton1 { host.listener?.didSelectRestoreMessageRecovery() }

                textButton2(host.stringProvider.getString(CommonStrings.keys_backup_settings_delete_backup_button))
                clickOnButton2 { host.listener?.didSelectDeleteSetupMessageRecovery() }
            } else {
                textButton1(host.stringProvider.getString(CommonStrings.keys_backup_setup))
                clickOnButton1 { host.listener?.didSelectSetupMessageRecovery() }
            }
        }
    }

    private fun buildKeysBackupTrust(keysVersionTrust: Async<KeysBackupVersionTrust>) {
        val host = this
        when (keysVersionTrust) {
            is Uninitialized -> Unit
            is Loading -> {
                loadingItem {
                    id("trust")
                }
            }
            is Success -> {
                keysVersionTrust()
                        .signatures
                        .filterIsInstance<KeysBackupVersionTrustSignature.UserSignature>()
                        .forEach {
                            val isUserVerified = it.cryptoCrossSigningKey?.trustLevel?.isVerified().orFalse()
                            val isSignatureValid = it.valid
                            val userId: String = it.cryptoCrossSigningKey?.userId ?: ""
                            if (userId == session.sessionParams.userId && isSignatureValid && isUserVerified) {
                                genericItem {
                                    id(UUID.randomUUID().toString())
                                    title(host.stringProvider.getString(CommonStrings.keys_backup_info_title_signature).toEpoxyCharSequence())
                                    description(
                                            host.stringProvider
                                                    .getString(CommonStrings.keys_backup_settings_signature_from_this_user)
                                                    .toEpoxyCharSequence()
                                    )
                                    endIconResourceId(R.drawable.e2e_verified)
                                }
                            }
                        }

                keysVersionTrust()
                        .signatures
                        .filterIsInstance<KeysBackupVersionTrustSignature.DeviceSignature>()
                        .forEach {
                            genericItem {
                                id(UUID.randomUUID().toString())
                                title(host.stringProvider.getString(CommonStrings.keys_backup_info_title_signature).toEpoxyCharSequence())

                                val isDeviceKnown = it.device != null
                                val isDeviceVerified = it.device?.isVerified ?: false
                                val isSignatureValid = it.valid
                                val deviceId: String = it.deviceId ?: ""

                                if (!isDeviceKnown) {
                                    description(
                                            host.stringProvider
                                                    .getString(CommonStrings.keys_backup_settings_signature_from_unknown_device, deviceId)
                                                    .toEpoxyCharSequence()
                                    )
                                    endIconResourceId(R.drawable.e2e_warning)
                                } else {
                                    if (isSignatureValid) {
                                        if (host.session.sessionParams.deviceId == it.deviceId) {
                                            description(
                                                    host.stringProvider
                                                            .getString(CommonStrings.keys_backup_settings_valid_signature_from_this_device)
                                                            .toEpoxyCharSequence()
                                            )
                                            endIconResourceId(R.drawable.e2e_verified)
                                        } else {
                                            if (isDeviceVerified) {
                                                description(
                                                        host.stringProvider
                                                                .getString(
                                                                        CommonStrings.keys_backup_settings_valid_signature_from_verified_device,
                                                                        deviceId
                                                                )
                                                                .toEpoxyCharSequence()
                                                )
                                                endIconResourceId(R.drawable.e2e_verified)
                                            } else {
                                                description(
                                                        host.stringProvider
                                                                .getString(
                                                                        CommonStrings.keys_backup_settings_valid_signature_from_unverified_device,
                                                                        deviceId
                                                                )
                                                                .toEpoxyCharSequence()
                                                )
                                                endIconResourceId(R.drawable.e2e_warning)
                                            }
                                        }
                                    } else {
                                        // Invalid signature
                                        endIconResourceId(R.drawable.e2e_warning)
                                        if (isDeviceVerified) {
                                            description(
                                                    host.stringProvider
                                                            .getString(
                                                                    CommonStrings.keys_backup_settings_invalid_signature_from_verified_device,
                                                                    deviceId
                                                            )
                                                            .toEpoxyCharSequence()
                                            )
                                        } else {
                                            description(
                                                    host.stringProvider
                                                            .getString(
                                                                    CommonStrings.keys_backup_settings_invalid_signature_from_unverified_device,
                                                                    deviceId
                                                            )
                                                            .toEpoxyCharSequence()
                                            )
                                        }
                                    }
                                }
                            }
                        } // end for each
            }
            is Fail -> {
                errorWithRetryItem {
                    id("trust")
                    text(host.stringProvider.getString(CommonStrings.keys_backup_unable_to_get_trust_info))
                    listener { host.listener?.loadTrustData() }
                }
            }
        }
    }

    interface Listener {
        fun didSelectSetupMessageRecovery()
        fun didSelectRestoreMessageRecovery()
        fun didSelectDeleteSetupMessageRecovery()
        fun loadTrustData()
        fun loadKeysBackupState()
    }
}
