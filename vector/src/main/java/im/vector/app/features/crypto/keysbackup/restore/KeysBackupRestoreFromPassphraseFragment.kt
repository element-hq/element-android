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
import android.text.Editable
import android.text.SpannableString
import android.text.style.ClickableSpan
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.core.text.set
import androidx.lifecycle.Observer
import butterknife.BindView
import butterknife.OnClick
import butterknife.OnTextChanged
import com.google.android.material.textfield.TextInputLayout
import im.vector.app.R
import im.vector.app.core.extensions.showPassword
import im.vector.app.core.platform.VectorBaseFragment
import javax.inject.Inject

class KeysBackupRestoreFromPassphraseFragment @Inject constructor() : VectorBaseFragment() {

    override fun getLayoutResId() = R.layout.fragment_keys_backup_restore_from_passphrase

    private lateinit var viewModel: KeysBackupRestoreFromPassphraseViewModel
    private lateinit var sharedViewModel: KeysBackupRestoreSharedViewModel

    @BindView(R.id.keys_backup_passphrase_enter_til)
    lateinit var mPassphraseInputLayout: TextInputLayout

    @BindView(R.id.keys_backup_passphrase_enter_edittext)
    lateinit var mPassphraseTextEdit: EditText

    @BindView(R.id.keys_backup_view_show_password)
    lateinit var mPassphraseReveal: ImageView

    @BindView(R.id.keys_backup_passphrase_help_with_link)
    lateinit var helperTextWithLink: TextView

    @OnClick(R.id.keys_backup_view_show_password)
    fun toggleVisibilityMode() {
        viewModel.showPasswordMode.value = !(viewModel.showPasswordMode.value ?: false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        viewModel = fragmentViewModelProvider.get(KeysBackupRestoreFromPassphraseViewModel::class.java)
        sharedViewModel = activityViewModelProvider.get(KeysBackupRestoreSharedViewModel::class.java)

        viewModel.passphraseErrorText.observe(viewLifecycleOwner, Observer { newValue ->
            mPassphraseInputLayout.error = newValue
        })

        helperTextWithLink.text = spannableStringForHelperText()

        viewModel.showPasswordMode.observe(viewLifecycleOwner, Observer {
            val shouldBeVisible = it ?: false
            mPassphraseTextEdit.showPassword(shouldBeVisible)
            mPassphraseReveal.setImageResource(if (shouldBeVisible) R.drawable.ic_eye_closed else R.drawable.ic_eye)
        })

        mPassphraseTextEdit.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                onRestoreBackup()
                return@setOnEditorActionListener true
            }
            return@setOnEditorActionListener false
        }
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

    @OnTextChanged(R.id.keys_backup_passphrase_enter_edittext)
    fun onPassphraseTextEditChange(s: Editable?) {
        s?.toString()?.let { viewModel.updatePassphrase(it) }
    }

    @OnClick(R.id.keys_backup_passphrase_help_with_link)
    fun onUseRecoveryKey() {
        sharedViewModel.moveToRecoverWithKey()
    }

    @OnClick(R.id.keys_backup_restore_with_passphrase_submit)
    fun onRestoreBackup() {
        val value = viewModel.passphrase.value
        if (value.isNullOrBlank()) {
            viewModel.passphraseErrorText.value = getString(R.string.passphrase_empty_error_message)
        } else {
            viewModel.recoverKeys(sharedViewModel)
        }
    }
}
