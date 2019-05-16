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
package im.vector.fragments.keysbackup.settings

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import butterknife.BindView
import im.vector.matrix.android.api.session.crypto.keysbackup.KeysBackupState
import im.vector.riotredesign.R
import im.vector.riotredesign.core.platform.VectorBaseFragment
import im.vector.riotredesign.core.platform.WaitingViewData
import im.vector.riotredesign.features.crypto.keysbackup.restore.KeysBackupRestoreActivity
import im.vector.riotredesign.features.crypto.keysbackup.settings.KeysBackupSettingsViewModel
import im.vector.riotredesign.features.crypto.keysbackup.setup.KeysBackupSetupActivity

class KeysBackupSettingsFragment : VectorBaseFragment(),
        KeysBackupSettingsRecyclerViewAdapter.AdapterListener {


    companion object {
        fun newInstance() = KeysBackupSettingsFragment()
    }

    override fun getLayoutResId() = R.layout.fragment_keys_backup_settings

    private lateinit var viewModel: KeysBackupSettingsViewModel

    @BindView(R.id.keys_backup_settings_recycler_view)
    lateinit var recyclerView: RecyclerView

    private var recyclerViewAdapter: KeysBackupSettingsRecyclerViewAdapter? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val layoutManager = LinearLayoutManager(context)
        recyclerView.layoutManager = layoutManager

        recyclerViewAdapter = KeysBackupSettingsRecyclerViewAdapter(activity!!)
        recyclerView.adapter = recyclerViewAdapter
        recyclerViewAdapter?.adapterListener = this


        viewModel = activity?.run {
            ViewModelProviders.of(this).get(KeysBackupSettingsViewModel::class.java)
        } ?: throw Exception("Invalid Activity")


        viewModel.keyBackupState.observe(this, Observer { keysBackupState ->
            if (keysBackupState == null) {
                //Cannot happen?
                viewModel.keyVersionTrust.value = null
            } else {
                when (keysBackupState) {
                    KeysBackupState.Unknown,
                    KeysBackupState.CheckingBackUpOnHomeserver -> {
                        viewModel.loadingEvent.value = WaitingViewData("")
                    }
                    else -> {
                        viewModel.loadingEvent.value = null
                        //All this cases will be manage by looking at the backup trust object
                        viewModel.session?.getKeysBackupService()?.mKeysBackupVersion?.let {
                            viewModel.getKeysBackupTrust(it)
                        } ?: run {
                            viewModel.keyVersionTrust.value = null
                        }
                    }
                }
            }

            // Update the adapter for each state change
            viewModel.session?.let { session ->
                recyclerViewAdapter?.updateWithTrust(session, viewModel.keyVersionTrust.value)
            }
        })

        viewModel.keyVersionTrust.observe(this, Observer {
            viewModel.session?.let { session ->
                recyclerViewAdapter?.updateWithTrust(session, it)
            }
        })

    }

    override fun didSelectSetupMessageRecovery() {
        context?.let {
            startActivity(KeysBackupSetupActivity.intent(it, false))
        }
    }

    override fun didSelectRestoreMessageRecovery() {
        context?.let {
            startActivity(KeysBackupRestoreActivity.intent(it))
        }
    }

    override fun didSelectDeleteSetupMessageRecovery() {
        activity?.let {
            AlertDialog.Builder(it)
                    .setTitle(R.string.keys_backup_settings_delete_confirm_title)
                    .setMessage(R.string.keys_backup_settings_delete_confirm_message)
                    .setCancelable(false)
                    .setPositiveButton(R.string.keys_backup_settings_delete_confirm_title) { _, _ ->
                        viewModel.deleteCurrentBackup(it)
                    }
                    .setNegativeButton(R.string.cancel, null)
                    .setCancelable(true)
                    .show()
        }
    }

}