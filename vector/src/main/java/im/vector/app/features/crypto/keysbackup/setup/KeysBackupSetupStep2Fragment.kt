/*
 * Copyright 2019 New Vector Ltd
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
package im.vector.app.features.crypto.keysbackup.setup

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.Observer
import androidx.lifecycle.viewModelScope
import androidx.transition.TransitionManager
import com.nulabinc.zxcvbn.Zxcvbn
import im.vector.app.R
import im.vector.app.core.extensions.showPassword
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.databinding.FragmentKeysBackupRestoreFromKeyBinding
import im.vector.app.databinding.FragmentKeysBackupSetupStep2Binding
import im.vector.app.features.settings.VectorLocale

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

class KeysBackupSetupStep2Fragment @Inject constructor() : VectorBaseFragment<FragmentKeysBackupSetupStep2Binding>() {

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentKeysBackupSetupStep2Binding {
        return FragmentKeysBackupSetupStep2Binding.inflate(inflater, container, false)
    }

    private val zxcvbn = Zxcvbn()

    private fun onPassphraseChanged() {
        viewModel.passphrase.value = keys_backup_passphrase_enter_edittext.text.toString()
        viewModel.confirmPassphraseError.value = null
    }

    private fun onConfirmPassphraseChanged() {
        viewModel.confirmPassphrase.value = mPassphraseConfirmTextEdit.text.toString()
    }

    private lateinit var viewModel: KeysBackupSetupSharedViewModel

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = activityViewModelProvider.get(KeysBackupSetupSharedViewModel::class.java)

        viewModel.shouldPromptOnBack = true
        bindViewToViewModel()
        setupViews()
    }

    /* ==========================================================================================
     * MENU
     * ========================================================================================== */

    private fun bindViewToViewModel() {
        viewModel.passwordStrength.observe(viewLifecycleOwner, Observer { strength ->
            if (strength == null) {
                mPassphraseProgressLevel.strength = 0
                keys_backup_passphrase_enter_til.error = null
            } else {
                val score = strength.score
                mPassphraseProgressLevel.strength = score

                if (score in 1..3) {
                    val warning = strength.feedback?.getWarning(VectorLocale.applicationLocale)
                    if (warning != null) {
                        keys_backup_passphrase_enter_til.error = warning
                    }

                    val suggestions = strength.feedback?.getSuggestions(VectorLocale.applicationLocale)
                    if (suggestions != null) {
                        keys_backup_passphrase_enter_til.error = suggestions.firstOrNull()
                    }
                } else {
                    keys_backup_passphrase_enter_til.error = null
                }
            }
        })

        viewModel.passphrase.observe(viewLifecycleOwner, Observer<String> { newValue ->
            if (newValue.isEmpty()) {
                viewModel.passwordStrength.value = null
            } else {
                viewModel.viewModelScope.launch(Dispatchers.IO) {
                    val strength = zxcvbn.measure(newValue)
                    launch(Dispatchers.Main) {
                        viewModel.passwordStrength.value = strength
                    }
                }
            }
        })

        keys_backup_passphrase_enter_edittext.setText(viewModel.passphrase.value)

        viewModel.passphraseError.observe(viewLifecycleOwner, Observer {
            TransitionManager.beginDelayedTransition(keys_backup_root)
            keys_backup_passphrase_enter_til.error = it
        })

        mPassphraseConfirmTextEdit.setText(viewModel.confirmPassphrase.value)

        viewModel.showPasswordMode.observe(viewLifecycleOwner, Observer {
            val shouldBeVisible = it ?: false
            keys_backup_passphrase_enter_edittext.showPassword(shouldBeVisible)
            mPassphraseConfirmTextEdit.showPassword(shouldBeVisible)
            keys_backup_view_show_password.setImageResource(if (shouldBeVisible) R.drawable.ic_eye_closed else R.drawable.ic_eye)
        })

        viewModel.confirmPassphraseError.observe(viewLifecycleOwner, Observer {
            TransitionManager.beginDelayedTransition(keys_backup_root)
            mPassphraseConfirmInputLayout.error = it
        })

        mPassphraseConfirmTextEdit.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                doNext()
                return@setOnEditorActionListener true
            }
            return@setOnEditorActionListener false
        }
    }

    private fun setupViews() {
        keys_backup_view_show_password.setOnClickListener { toggleVisibilityMode() }
        keys_backup_setup_step2_button.setOnClickListener { doNext() }
        keys_backup_setup_step2_skip_button.setOnClickListener { skipPassphrase() }

        keys_backup_passphrase_enter_edittext.doOnTextChanged { _, _, _, _ ->  onPassphraseChanged() }
        mPassphraseConfirmTextEdit.doOnTextChanged { _, _, _, _ ->  onConfirmPassphraseChanged() }
    }

    private fun toggleVisibilityMode() {
        viewModel.showPasswordMode.value = !(viewModel.showPasswordMode.value ?: false)
    }

    private fun doNext() {
        when {
            viewModel.passphrase.value.isNullOrEmpty()                      -> {
                viewModel.passphraseError.value = context?.getString(R.string.passphrase_empty_error_message)
            }
            viewModel.passphrase.value != viewModel.confirmPassphrase.value -> {
                viewModel.confirmPassphraseError.value = context?.getString(R.string.passphrase_passphrase_does_not_match)
            }
            viewModel.passwordStrength.value?.score ?: 0 < 4                -> {
                viewModel.passphraseError.value = context?.getString(R.string.passphrase_passphrase_too_weak)
            }
            else                                                            -> {
                viewModel.megolmBackupCreationInfo = null

                viewModel.prepareRecoveryKey(requireActivity(), viewModel.passphrase.value)
            }
        }
    }

    private fun skipPassphrase() {
        when {
            viewModel.passphrase.value.isNullOrEmpty() -> {
                // Generate a recovery key for the user
                viewModel.megolmBackupCreationInfo = null

                viewModel.prepareRecoveryKey(requireActivity(), null)
            }
            else                                       -> {
                // User has entered a passphrase but want to skip this step.
                viewModel.passphraseError.value = context?.getString(R.string.keys_backup_passphrase_not_empty_error_message)
            }
        }
    }
}
