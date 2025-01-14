/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.crypto.recover

import android.app.Activity
import android.os.Bundle
import android.text.InputType.TYPE_CLASS_TEXT
import android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
import android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.core.text.toSpannable
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.airbnb.mvrx.parentFragmentViewModel
import com.airbnb.mvrx.withState
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.core.extensions.hideKeyboard
import im.vector.app.core.extensions.registerStartForActivityResult
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.core.resources.ColorProvider
import im.vector.app.core.utils.colorizeMatchingText
import im.vector.app.core.utils.startImportTextFromFileIntent
import im.vector.app.databinding.FragmentBootstrapMigrateBackupBinding
import im.vector.lib.core.utils.flow.throttleFirst
import im.vector.lib.strings.CommonStrings
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.matrix.android.sdk.api.extensions.tryOrNull
import org.matrix.android.sdk.api.session.crypto.keysbackup.isValidRecoveryKey
import reactivecircus.flowbinding.android.widget.editorActionEvents
import reactivecircus.flowbinding.android.widget.textChanges
import javax.inject.Inject

@AndroidEntryPoint
class BootstrapMigrateBackupFragment :
        VectorBaseFragment<FragmentBootstrapMigrateBackupBinding>() {

    @Inject lateinit var colorProvider: ColorProvider

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentBootstrapMigrateBackupBinding {
        return FragmentBootstrapMigrateBackupBinding.inflate(inflater, container, false)
    }

    val sharedViewModel: BootstrapSharedViewModel by parentFragmentViewModel()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        withState(sharedViewModel) {
            // set initial value (useful when coming back)
            views.bootstrapMigrateEditText.setText(it.passphrase ?: "")
        }
        views.bootstrapMigrateEditText.editorActionEvents()
                .throttleFirst(300)
                .onEach {
                    if (it.actionId == EditorInfo.IME_ACTION_DONE) {
                        submit()
                    }
                }
                .launchIn(viewLifecycleOwner.lifecycleScope)

        views.bootstrapMigrateEditText.textChanges()
                .skipInitialValue()
                .onEach {
                    views.bootstrapRecoveryKeyEnterTil.error = null
                    // sharedViewModel.handle(BootstrapActions.UpdateCandidatePassphrase(it?.toString() ?: ""))
                }
                .launchIn(viewLifecycleOwner.lifecycleScope)

        // sharedViewModel.observeViewEvents {}
        views.bootstrapMigrateContinueButton.debouncedClicks { submit() }
        views.bootstrapMigrateForgotPassphrase.debouncedClicks { sharedViewModel.handle(BootstrapActions.HandleForgotBackupPassphrase) }
        views.bootstrapMigrateUseFile.debouncedClicks { startImportTextFromFileIntent(requireContext(), importFileStartForActivityResult) }
    }

    private fun submit() = withState(sharedViewModel) { state ->
        val getBackupSecretForMigration = state.step as? BootstrapStep.GetBackupSecretForMigration ?: return@withState

        val isEnteringKey = getBackupSecretForMigration.useKey()

        val secret = views.bootstrapMigrateEditText.text?.toString()
        if (secret.isNullOrEmpty()) {
            val errRes = if (isEnteringKey) CommonStrings.recovery_key_empty_error_message else CommonStrings.passphrase_empty_error_message
            views.bootstrapRecoveryKeyEnterTil.error = getString(errRes)
        } else if (isEnteringKey && !isValidRecoveryKey(secret)) {
            views.bootstrapRecoveryKeyEnterTil.error = getString(CommonStrings.bootstrap_invalid_recovery_key)
        } else {
            view?.hideKeyboard()
            if (isEnteringKey) {
                sharedViewModel.handle(BootstrapActions.DoMigrateWithRecoveryKey(secret))
            } else {
                sharedViewModel.handle(BootstrapActions.DoMigrateWithPassphrase(secret))
            }
        }
    }

    override fun invalidate() = withState(sharedViewModel) { state ->
        val getBackupSecretForMigration = state.step as? BootstrapStep.GetBackupSecretForMigration ?: return@withState

        val isEnteringKey = getBackupSecretForMigration.useKey()

        if (isEnteringKey) {
            views.bootstrapMigrateEditText.inputType = TYPE_CLASS_TEXT or TYPE_TEXT_VARIATION_VISIBLE_PASSWORD or TYPE_TEXT_FLAG_MULTI_LINE

            val recKey = getString(CommonStrings.bootstrap_migration_backup_recovery_key)
            views.bootstrapDescriptionText.text = getString(CommonStrings.enter_account_password, recKey)

            views.bootstrapMigrateEditText.hint = recKey

            views.bootstrapMigrateEditText.hint = recKey
            views.bootstrapMigrateForgotPassphrase.isVisible = false
            views.bootstrapMigrateUseFile.isVisible = true
        } else {
            views.bootstrapDescriptionText.text = getString(CommonStrings.bootstrap_migration_enter_backup_password)

            views.bootstrapMigrateEditText.hint = getString(CommonStrings.passphrase_enter_passphrase)

            views.bootstrapMigrateForgotPassphrase.isVisible = true

            val recKey = getString(CommonStrings.bootstrap_migration_use_recovery_key)
            views.bootstrapMigrateForgotPassphrase.text = getString(CommonStrings.bootstrap_migration_with_passphrase_helper_with_link, recKey)
                    .toSpannable()
                    .colorizeMatchingText(recKey, colorProvider.getColorFromAttribute(android.R.attr.textColorLink))

            views.bootstrapMigrateUseFile.isVisible = false
        }
        views.bootstrapDescriptionText.giveAccessibilityFocusOnce()
    }

    private val importFileStartForActivityResult = registerStartForActivityResult { activityResult ->
        if (activityResult.resultCode == Activity.RESULT_OK) {
            activityResult.data?.data?.let { dataURI ->
                tryOrNull {
                    activity?.contentResolver?.openInputStream(dataURI)
                            ?.bufferedReader()
                            ?.use { it.readText() }
                            ?.let {
                                views.bootstrapMigrateEditText.setText(it)
                            }
                }
            }
        }
    }
}
