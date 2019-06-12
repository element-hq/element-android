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
package im.vector.riotredesign.features.crypto.keysbackup.setup

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import butterknife.BindView
import butterknife.OnClick
import com.google.android.material.bottomsheet.BottomSheetDialog
import im.vector.fragments.keysbackup.setup.KeysBackupSetupSharedViewModel
import im.vector.riotredesign.R
import im.vector.riotredesign.core.platform.VectorBaseFragment
import im.vector.riotredesign.core.utils.*
import java.io.ByteArrayInputStream

class KeysBackupSetupStep3Fragment : VectorBaseFragment() {

    override fun getLayoutResId() = R.layout.fragment_keys_backup_setup_step3

    @BindView(R.id.keys_backup_setup_step3_button)
    lateinit var mFinishButton: Button

    @BindView(R.id.keys_backup_recovery_key_text)
    lateinit var mRecoveryKeyTextView: TextView

    @BindView(R.id.keys_backup_setup_step3_line2_text)
    lateinit var mRecoveryKeyLabel2TextView: TextView

    companion object {
        fun newInstance() = KeysBackupSetupStep3Fragment()
    }

    private lateinit var viewModel: KeysBackupSetupSharedViewModel

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = activity?.run {
            ViewModelProviders.of(this).get(KeysBackupSetupSharedViewModel::class.java)
        } ?: throw Exception("Invalid Activity")


        viewModel.shouldPromptOnBack = false

        viewModel.passphrase.observe(this, Observer {
            if (it.isNullOrBlank()) {
                //Recovery was generated, so show key and options to save
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
            //nothing
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
        val dialog = BottomSheetDialog(activity!!)
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

                it.setOnClickListener {
                    copyToClipboard(activity!!, recoveryKey)
                }
            }
        }

        dialog.findViewById<View>(R.id.keys_backup_setup_save)?.setOnClickListener {
            if (checkPermissions(PERMISSIONS_FOR_WRITING_FILES, this, PERMISSION_REQUEST_CODE_EXPORT_KEYS)) {
                exportRecoveryKeyToFile(recoveryKey)
            }
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

            copyToClipboard(activity!!, it)
        }
    }

    fun exportRecoveryKeyToFile(it: String) {
        val stream = ByteArrayInputStream(it.toByteArray())

        vectorBaseActivity.notImplemented("export recovery key to file")
        /*
        val url = viewModel.session.mediaCache.saveMedia(stream, "recovery-key" + System.currentTimeMillis() + ".txt", "text/plain")
        stream.close()
        CommonActivityUtils.saveMediaIntoDownloads(context,
                File(Uri.parse(url).path!!), "recovery-key.txt", "text/plain", object : SimpleApiCallback<String>() {
            override fun onSuccess(path: String) {
                context?.let {
                    AlertDialog.Builder(it)
                            .setMessage(getString(R.string.recovery_key_export_saved_as_warning, path))
                            .setCancelable(false)
                            .setPositiveButton(R.string.ok, null)
                            .show()
                }

                viewModel.copyHasBeenMade = true
            }
        })
        */
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (allGranted(grantResults)) {
            if (requestCode == PERMISSION_REQUEST_CODE_EXPORT_KEYS) {
                viewModel.recoveryKey.value?.let {
                    exportRecoveryKeyToFile(it)
                }
            }
        }
    }
}
