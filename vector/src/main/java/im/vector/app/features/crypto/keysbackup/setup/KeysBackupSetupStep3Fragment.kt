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

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import arrow.core.Try
import butterknife.BindView
import butterknife.OnClick
import com.google.android.material.bottomsheet.BottomSheetDialog
import im.vector.app.R
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.core.utils.LiveEvent
import im.vector.app.core.utils.copyToClipboard
import im.vector.app.core.utils.selectTxtFileToWrite
import im.vector.app.core.utils.startSharePlainTextIntent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

class KeysBackupSetupStep3Fragment @Inject constructor() : VectorBaseFragment() {

    companion object {
        private const val SAVE_RECOVERY_KEY_REQUEST_CODE = 2754
    }

    override fun getLayoutResId() = R.layout.fragment_keys_backup_setup_step3

    @BindView(R.id.keys_backup_setup_step3_button)
    lateinit var mFinishButton: Button

    @BindView(R.id.keys_backup_recovery_key_text)
    lateinit var mRecoveryKeyTextView: TextView

    @BindView(R.id.keys_backup_setup_step3_line2_text)
    lateinit var mRecoveryKeyLabel2TextView: TextView

    private lateinit var viewModel: KeysBackupSetupSharedViewModel

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = activityViewModelProvider.get(KeysBackupSetupSharedViewModel::class.java)

        viewModel.shouldPromptOnBack = false

        viewModel.passphrase.observe(viewLifecycleOwner, Observer {
            if (it.isNullOrBlank()) {
                // Recovery was generated, so show key and options to save
                mRecoveryKeyLabel2TextView.text = getString(R.string.keys_backup_setup_step3_text_line2_no_passphrase)
                mFinishButton.text = getString(R.string.keys_backup_setup_step3_button_title_no_passphrase)

                mRecoveryKeyTextView.text = viewModel.recoveryKey.value!!
                        .replace(" ", "")
                        .chunked(16)
                        .joinToString("\n") {
                            it
                                    .chunked(4)
                                    .joinToString(" ")
                        }
                mRecoveryKeyTextView.isVisible = true
            } else {
                mRecoveryKeyLabel2TextView.text = getString(R.string.keys_backup_setup_step3_text_line2)
                mFinishButton.text = getString(R.string.keys_backup_setup_step3_button_title)
                mRecoveryKeyTextView.isVisible = false
            }
        })
    }

    @OnClick(R.id.keys_backup_setup_step3_button)
    fun onFinishButtonClicked() {
        if (viewModel.megolmBackupCreationInfo == null) {
            // nothing
        } else {
            if (viewModel.passphrase.value.isNullOrBlank() && !viewModel.copyHasBeenMade) {
                Toast.makeText(context, R.string.keys_backup_setup_step3_please_make_copy, Toast.LENGTH_LONG).show()
            } else {
                viewModel.navigateEvent.value = LiveEvent(KeysBackupSetupSharedViewModel.NAVIGATE_FINISH)
            }
        }
    }

    @OnClick(R.id.keys_backup_setup_step3_copy_button)
    fun onCopyButtonClicked() {
        val dialog = BottomSheetDialog(requireActivity())
        dialog.setContentView(R.layout.bottom_sheet_save_recovery_key)
        dialog.setCanceledOnTouchOutside(true)
        val recoveryKey = viewModel.recoveryKey.value!!

        if (viewModel.passphrase.value.isNullOrBlank()) {
            dialog.findViewById<TextView>(R.id.keys_backup_recovery_key_text)?.isVisible = false
        } else {
            dialog.findViewById<TextView>(R.id.keys_backup_recovery_key_text)?.let {
                it.isVisible = true
                it.text = recoveryKey.replace(" ", "")
                        .chunked(16)
                        .joinToString("\n") {
                            it
                                    .chunked(4)
                                    .joinToString(" ")
                        }

                it.debouncedClicks {
                    copyToClipboard(requireActivity(), recoveryKey)
                }
            }
        }

        dialog.findViewById<View>(R.id.keys_backup_setup_save)?.setOnClickListener {
            val userId = viewModel.userId
            val timestamp = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            selectTxtFileToWrite(
                    activity = requireActivity(),
                    fragment = this,
                    defaultFileName = "recovery-key-$userId-$timestamp.txt",
                    chooserHint = getString(R.string.save_recovery_key_chooser_hint),
                    requestCode = SAVE_RECOVERY_KEY_REQUEST_CODE
            )
            dialog.dismiss()
        }

        dialog.findViewById<View>(R.id.keys_backup_setup_share)?.setOnClickListener {
            startSharePlainTextIntent(this,
                    context?.getString(R.string.keys_backup_setup_step3_share_intent_chooser_title),
                    recoveryKey,
                    context?.getString(R.string.recovery_key))
            viewModel.copyHasBeenMade = true
            dialog.dismiss()
        }

        dialog.show()
    }

    @OnClick(R.id.keys_backup_recovery_key_text)
    fun onRecoveryKeyClicked() {
        viewModel.recoveryKey.value?.let {
            viewModel.copyHasBeenMade = true

            copyToClipboard(requireActivity(), it)
        }
    }

    private fun exportRecoveryKeyToFile(uri: Uri, data: String) {
        GlobalScope.launch(Dispatchers.Main) {
            Try {
                withContext(Dispatchers.IO) {
                    requireContext().contentResolver.openOutputStream(uri)
                            ?.use { os ->
                                os.write(data.toByteArray())
                                os.flush()
                            }
                }
                        ?: throw IOException("Unable to write the file")
            }
                    .fold(
                            { throwable ->
                                activity?.let {
                                    AlertDialog.Builder(it)
                                            .setTitle(R.string.dialog_title_error)
                                            .setMessage(errorFormatter.toHumanReadable(throwable))
                                }
                            },
                            {
                                viewModel.copyHasBeenMade = true
                                activity?.let {
                                    AlertDialog.Builder(it)
                                            .setTitle(R.string.dialog_title_success)
                                            .setMessage(R.string.recovery_key_export_saved)
                                }
                            }
                    )
                    ?.setCancelable(false)
                    ?.setPositiveButton(R.string.ok, null)
                    ?.show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            SAVE_RECOVERY_KEY_REQUEST_CODE -> {
                val uri = data?.data
                if (resultCode == Activity.RESULT_OK && uri != null) {
                    viewModel.recoveryKey.value?.let {
                        exportRecoveryKeyToFile(uri, it)
                    }
                }
            }
        }
    }
}
