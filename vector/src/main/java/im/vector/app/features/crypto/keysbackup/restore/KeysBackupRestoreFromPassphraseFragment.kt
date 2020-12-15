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

import android.os.Bundle
import android.text.SpannableString
import android.text.style.ClickableSpan
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.core.text.set
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.Observer
import im.vector.app.R
import im.vector.app.core.extensions.showPassword
import im.vector.app.core.platform.VectorBaseFragment
import kotlinx.android.synthetic.main.fragment_keys_backup_restore_from_passphrase.*
import javax.inject.Inject

class KeysBackupRestoreFromPassphraseFragment @Inject constructor() : VectorBaseFragment() {

    override fun getLayoutResId() = R.layout.fragment_keys_backup_restore_from_passphrase

    private lateinit var viewModel: KeysBackupRestoreFromPassphraseViewModel
    private lateinit var sharedViewModel: KeysBackupRestoreSharedViewModel

    private fun toggleVisibilityMode() {
        viewModel.showPasswordMode.value = !(viewModel.showPasswordMode.value ?: false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = fragmentViewModelProvider.get(KeysBackupRestoreFromPassphraseViewModel::class.java)
        sharedViewModel = activityViewModelProvider.get(KeysBackupRestoreSharedViewModel::class.java)

        viewModel.passphraseErrorText.observe(viewLifecycleOwner, Observer { newValue ->
            keys_backup_passphrase_enter_til.error = newValue
        })

        helperTextWithLink.text = spannableStringForHelperText()

        viewModel.showPasswordMode.observe(viewLifecycleOwner, Observer {
            val shouldBeVisible = it ?: false
            keys_backup_passphrase_enter_edittext.showPassword(shouldBeVisible)
            keys_backup_view_show_password.setImageResource(if (shouldBeVisible) R.drawable.ic_eye_closed else R.drawable.ic_eye)
        })

        keys_backup_passphrase_enter_edittext.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                onRestoreBackup()
                return@setOnEditorActionListener true
            }
            return@setOnEditorActionListener false
        }

        keys_backup_view_show_password.setOnClickListener { toggleVisibilityMode() }
        helperTextWithLink.setOnClickListener { onUseRecoveryKey() }
        keys_backup_restore_with_passphrase_submit.setOnClickListener { onRestoreBackup() }
        keys_backup_passphrase_enter_edittext.doOnTextChanged { text, _, _, _ -> onPassphraseTextEditChange(text) }
    }

    private fun spannableStringForHelperText(): SpannableString {
        val clickableText = getString(R.string.keys_backup_restore_use_recovery_key)
        val helperText = getString(R.string.keys_backup_restore_with_passphrase_helper_with_link, clickableText)

        val spanString = SpannableString(helperText)

        // used just to have default link representation
        val clickableSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {}
        }
        val start = helperText.indexOf(clickableText)
        val end = start + clickableText.length
        spanString[start, end] = clickableSpan
        return spanString
    }

    private fun onPassphraseTextEditChange(s: CharSequence?) {
        s?.toString()?.let { viewModel.updatePassphrase(it) }
    }

    private fun onUseRecoveryKey() {
        sharedViewModel.moveToRecoverWithKey()
    }

    private fun onRestoreBackup() {
        val value = viewModel.passphrase.value
        if (value.isNullOrBlank()) {
            viewModel.passphraseErrorText.value = getString(R.string.passphrase_empty_error_message)
        } else {
            viewModel.recoverKeys(sharedViewModel)
        }
    }
}
