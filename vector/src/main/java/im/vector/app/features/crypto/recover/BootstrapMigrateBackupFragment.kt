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

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.InputType.TYPE_CLASS_TEXT
import android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
import android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.core.text.toSpannable
import androidx.core.view.isVisible
import com.airbnb.mvrx.parentFragmentViewModel
import com.airbnb.mvrx.withState
import com.jakewharton.rxbinding3.widget.editorActionEvents
import com.jakewharton.rxbinding3.widget.textChanges
import im.vector.app.R
import im.vector.app.core.extensions.hideKeyboard
import im.vector.app.core.extensions.showPassword
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.core.resources.ColorProvider
import im.vector.app.core.utils.colorizeMatchingText
import im.vector.app.core.utils.startImportTextFromFileIntent
import org.matrix.android.sdk.api.extensions.tryThis
import org.matrix.android.sdk.internal.crypto.keysbackup.util.isValidRecoveryKey
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.android.synthetic.main.fragment_bootstrap_enter_passphrase.bootstrapDescriptionText
import kotlinx.android.synthetic.main.fragment_bootstrap_migrate_backup.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class BootstrapMigrateBackupFragment @Inject constructor(
        private val colorProvider: ColorProvider
) : VectorBaseFragment() {

    override fun getLayoutResId() = R.layout.fragment_bootstrap_migrate_backup

    val sharedViewModel: BootstrapSharedViewModel by parentFragmentViewModel()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        withState(sharedViewModel) {
            // set initial value (useful when coming back)
            bootstrapMigrateEditText.setText(it.passphrase ?: "")
        }
        bootstrapMigrateEditText.editorActionEvents()
                .throttleFirst(300, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    if (it.actionId == EditorInfo.IME_ACTION_DONE) {
                        submit()
                    }
                }
                .disposeOnDestroyView()

        bootstrapMigrateEditText.textChanges()
                .skipInitialValue()
                .subscribe {
                    bootstrapRecoveryKeyEnterTil.error = null
                    // sharedViewModel.handle(BootstrapActions.UpdateCandidatePassphrase(it?.toString() ?: ""))
                }
                .disposeOnDestroyView()

        // sharedViewModel.observeViewEvents {}
        bootstrapMigrateContinueButton.debouncedClicks { submit() }
        bootstrapMigrateShowPassword.debouncedClicks { sharedViewModel.handle(BootstrapActions.TogglePasswordVisibility) }
        bootstrapMigrateForgotPassphrase.debouncedClicks { sharedViewModel.handle(BootstrapActions.HandleForgotBackupPassphrase) }
        bootstrapMigrateUseFile.debouncedClicks { startImportTextFromFileIntent(this, IMPORT_FILE_REQ) }
    }

    private fun submit() = withState(sharedViewModel) { state ->
        val getBackupSecretForMigration = state.step as? BootstrapStep.GetBackupSecretForMigration ?: return@withState

        val isEnteringKey = getBackupSecretForMigration.useKey()

        val secret = bootstrapMigrateEditText.text?.toString()
        if (secret.isNullOrEmpty()) {
            val errRes = if (isEnteringKey) R.string.recovery_key_empty_error_message else R.string.passphrase_empty_error_message
            bootstrapRecoveryKeyEnterTil.error = getString(errRes)
        } else if (isEnteringKey && !isValidRecoveryKey(secret)) {
            bootstrapRecoveryKeyEnterTil.error = getString(R.string.bootstrap_invalid_recovery_key)
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
            bootstrapMigrateShowPassword.isVisible = false
            bootstrapMigrateEditText.inputType = TYPE_CLASS_TEXT or TYPE_TEXT_VARIATION_VISIBLE_PASSWORD or TYPE_TEXT_FLAG_MULTI_LINE

            val recKey = getString(R.string.bootstrap_migration_backup_recovery_key)
            bootstrapDescriptionText.text = getString(R.string.enter_account_password, recKey)

            bootstrapMigrateEditText.hint = recKey

            bootstrapMigrateEditText.hint = recKey
            bootstrapMigrateForgotPassphrase.isVisible = false
            bootstrapMigrateUseFile.isVisible = true
        } else {
            bootstrapMigrateShowPassword.isVisible = true

            if (state.step is BootstrapStep.GetBackupSecretPassForMigration) {
                val isPasswordVisible = state.step.isPasswordVisible
                bootstrapMigrateEditText.showPassword(isPasswordVisible, updateCursor = false)
                bootstrapMigrateShowPassword.setImageResource(if (isPasswordVisible) R.drawable.ic_eye_closed else R.drawable.ic_eye)
            }

            bootstrapDescriptionText.text = getString(R.string.bootstrap_migration_enter_backup_password)

            bootstrapMigrateEditText.hint = getString(R.string.passphrase_enter_passphrase)

            bootstrapMigrateForgotPassphrase.isVisible = true

            val recKey = getString(R.string.bootstrap_migration_use_recovery_key)
            bootstrapMigrateForgotPassphrase.text = getString(R.string.bootstrap_migration_with_passphrase_helper_with_link, recKey)
                    .toSpannable()
                    .colorizeMatchingText(recKey, colorProvider.getColorFromAttribute(android.R.attr.textColorLink))

            bootstrapMigrateUseFile.isVisible = false
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == IMPORT_FILE_REQ && resultCode == Activity.RESULT_OK) {
            data?.data?.let { dataURI ->
                tryThis {
                    activity?.contentResolver?.openInputStream(dataURI)
                            ?.bufferedReader()
                            ?.use { it.readText() }
                            ?.let {
                                bootstrapMigrateEditText.setText(it)
                            }
                }
            }
            return
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    companion object {
        private const val IMPORT_FILE_REQ = 0
    }
}
