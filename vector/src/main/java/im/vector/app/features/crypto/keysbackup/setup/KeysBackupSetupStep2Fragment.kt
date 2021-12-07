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
import androidx.lifecycle.viewModelScope
import androidx.transition.TransitionManager
import com.nulabinc.zxcvbn.Zxcvbn
import im.vector.app.R
import im.vector.app.core.extensions.hidePassword
import im.vector.app.core.platform.VectorBaseFragment
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
        viewModel.passphrase.value = views.keysBackupSetupStep2PassphraseEnterEdittext.text.toString()
        viewModel.confirmPassphraseError.value = null
    }

    private fun onConfirmPassphraseChanged() {
        viewModel.confirmPassphrase.value = views.keysBackupSetupStep2PassphraseConfirmEditText.text.toString()
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
        viewModel.passwordStrength.observe(viewLifecycleOwner) { strength ->
            if (strength == null) {
                views.keysBackupSetupStep2PassphraseStrengthLevel.strength = 0
                views.keysBackupSetupStep2PassphraseEnterTil.error = null
            } else {
                val score = strength.score
                views.keysBackupSetupStep2PassphraseStrengthLevel.strength = score

                if (score in 1..3) {
                    val warning = strength.feedback?.getWarning(VectorLocale.applicationLocale)
                    if (warning != null) {
                        views.keysBackupSetupStep2PassphraseEnterTil.error = warning
                    }

                    val suggestions = strength.feedback?.getSuggestions(VectorLocale.applicationLocale)
                    if (suggestions != null) {
                        views.keysBackupSetupStep2PassphraseEnterTil.error = suggestions.firstOrNull()
                    }
                } else {
                    views.keysBackupSetupStep2PassphraseEnterTil.error = null
                }
            }
        }

        viewModel.passphrase.observe(viewLifecycleOwner) { newValue ->
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
        }

        views.keysBackupSetupStep2PassphraseEnterEdittext.setText(viewModel.passphrase.value)

        viewModel.passphraseError.observe(viewLifecycleOwner) {
            TransitionManager.beginDelayedTransition(views.keysBackupRoot)
            views.keysBackupSetupStep2PassphraseEnterTil.error = it
        }

        views.keysBackupSetupStep2PassphraseConfirmEditText.setText(viewModel.confirmPassphrase.value)

        viewModel.confirmPassphraseError.observe(viewLifecycleOwner) {
            TransitionManager.beginDelayedTransition(views.keysBackupRoot)
            views.keysBackupSetupStep2PassphraseConfirmTil.error = it
        }

        views.keysBackupSetupStep2PassphraseConfirmEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                doNext()
                return@setOnEditorActionListener true
            }
            return@setOnEditorActionListener false
        }
    }

    private fun setupViews() {
        views.keysBackupSetupStep2Button.debouncedClicks { doNext() }
        views.keysBackupSetupStep2SkipButton.debouncedClicks { skipPassphrase() }

        views.keysBackupSetupStep2PassphraseEnterEdittext.doOnTextChanged { _, _, _, _ -> onPassphraseChanged() }
        views.keysBackupSetupStep2PassphraseConfirmEditText.doOnTextChanged { _, _, _, _ -> onConfirmPassphraseChanged() }
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

                // Ensure passphrase is hidden during the process
                views.keysBackupSetupStep2PassphraseEnterEdittext.hidePassword()
                views.keysBackupSetupStep2PassphraseConfirmEditText.hidePassword()
                viewModel.prepareRecoveryKey(requireActivity(), viewModel.passphrase.value)
            }
        }
    }

    private fun skipPassphrase() {
        when {
            viewModel.passphrase.value.isNullOrEmpty() -> {
                // Generate a recovery key for the user
                viewModel.megolmBackupCreationInfo = null

                // Ensure passphrase is hidden during the process
                views.keysBackupSetupStep2PassphraseEnterEdittext.hidePassword()
                views.keysBackupSetupStep2PassphraseConfirmEditText.hidePassword()
                viewModel.prepareRecoveryKey(requireActivity(), null)
            }
            else                                       -> {
                // User has entered a passphrase but want to skip this step.
                viewModel.passphraseError.value = context?.getString(R.string.keys_backup_passphrase_not_empty_error_message)
            }
        }
    }
}
