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
package im.vector.app.features.crypto.keysbackup.restore

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.core.widget.doOnTextChanged
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.R
import im.vector.app.core.extensions.registerStartForActivityResult
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.core.utils.startImportTextFromFileIntent
import im.vector.app.databinding.FragmentKeysBackupRestoreFromKeyBinding
import org.matrix.android.sdk.api.extensions.tryOrNull

@AndroidEntryPoint
class KeysBackupRestoreFromKeyFragment :
        VectorBaseFragment<FragmentKeysBackupRestoreFromKeyBinding>() {

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentKeysBackupRestoreFromKeyBinding {
        return FragmentKeysBackupRestoreFromKeyBinding.inflate(inflater, container, false)
    }

    private lateinit var viewModel: KeysBackupRestoreFromKeyViewModel
    private lateinit var sharedViewModel: KeysBackupRestoreSharedViewModel

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = fragmentViewModelProvider.get(KeysBackupRestoreFromKeyViewModel::class.java)
        sharedViewModel = activityViewModelProvider.get(KeysBackupRestoreSharedViewModel::class.java)
        views.keyTextEdit.setText(viewModel.recoveryCode.value)
        views.keyTextEdit.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                onRestoreFromKey()
                return@setOnEditorActionListener true
            }
            return@setOnEditorActionListener false
        }

        views.keyInputLayout.error = viewModel.recoveryCodeErrorText.value
        viewModel.recoveryCodeErrorText.observe(viewLifecycleOwner) { newValue ->
            views.keyInputLayout.error = newValue
        }

        views.keysRestoreButton.debouncedClicks { onRestoreFromKey() }
        views.keysBackupImport.debouncedClicks { onImport() }
        views.keyTextEdit.doOnTextChanged { text, _, _, _ -> onRestoreKeyTextEditChange(text) }
    }

    private fun onRestoreKeyTextEditChange(s: CharSequence?) {
        s?.toString()?.let {
            viewModel.updateCode(it)
        }
    }

    private fun onRestoreFromKey() {
        val value = viewModel.recoveryCode.value
        if (value.isNullOrBlank()) {
            viewModel.recoveryCodeErrorText.value = context?.getString(R.string.keys_backup_recovery_code_empty_error_message)
        } else {
            viewModel.recoverKeys(sharedViewModel)
        }
    }

    private fun onImport() {
        startImportTextFromFileIntent(requireContext(), textFileStartForActivityResult)
    }

    private val textFileStartForActivityResult = registerStartForActivityResult { activityResult ->
        if (activityResult.resultCode == Activity.RESULT_OK) {
            val dataURI = activityResult.data?.data ?: return@registerStartForActivityResult
            tryOrNull(message = "Failed to read recovery kay from text") {
                activity
                        ?.contentResolver
                        ?.openInputStream(dataURI)
                        ?.bufferedReader()
                        ?.use { it.readText() }
                        ?.let {
                            views.keyTextEdit.setText(it)
                            views.keyTextEdit.setSelection(it.length)
                        }
            }
        }
    }
}
