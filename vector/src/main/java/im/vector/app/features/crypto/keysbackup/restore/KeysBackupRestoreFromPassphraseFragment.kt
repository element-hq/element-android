/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package im.vector.app.features.crypto.keysbackup.restore

import android.os.Bundle
import android.text.SpannableString
import android.text.style.ClickableSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.core.text.set
import androidx.core.widget.doOnTextChanged
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.databinding.FragmentKeysBackupRestoreFromPassphraseBinding
import im.vector.lib.strings.CommonStrings

@AndroidEntryPoint
class KeysBackupRestoreFromPassphraseFragment :
        VectorBaseFragment<FragmentKeysBackupRestoreFromPassphraseBinding>() {

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentKeysBackupRestoreFromPassphraseBinding {
        return FragmentKeysBackupRestoreFromPassphraseBinding.inflate(inflater, container, false)
    }

    private lateinit var viewModel: KeysBackupRestoreFromPassphraseViewModel
    private lateinit var sharedViewModel: KeysBackupRestoreSharedViewModel

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = fragmentViewModelProvider.get(KeysBackupRestoreFromPassphraseViewModel::class.java)
        sharedViewModel = activityViewModelProvider.get(KeysBackupRestoreSharedViewModel::class.java)

        viewModel.passphraseErrorText.observe(viewLifecycleOwner) { newValue ->
            views.keysBackupPassphraseEnterTil.error = newValue
        }

        views.helperTextWithLink.text = spannableStringForHelperText()

        views.keysBackupPassphraseEnterEdittext.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                onRestoreBackup()
                return@setOnEditorActionListener true
            }
            return@setOnEditorActionListener false
        }

        views.helperTextWithLink.debouncedClicks { onUseRecoveryKey() }
        views.keysBackupRestoreWithPassphraseSubmit.debouncedClicks { onRestoreBackup() }
        views.keysBackupPassphraseEnterEdittext.doOnTextChanged { text, _, _, _ -> onPassphraseTextEditChange(text) }
    }

    private fun spannableStringForHelperText(): SpannableString {
        val clickableText = getString(CommonStrings.keys_backup_restore_use_recovery_key)
        val helperText = getString(CommonStrings.keys_backup_restore_with_passphrase_helper_with_link, clickableText)

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
            viewModel.passphraseErrorText.value = getString(CommonStrings.passphrase_empty_error_message)
        } else {
            viewModel.recoverKeys(sharedViewModel)
        }
    }
}
