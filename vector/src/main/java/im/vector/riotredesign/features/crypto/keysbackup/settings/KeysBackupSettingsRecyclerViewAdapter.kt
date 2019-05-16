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

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import butterknife.BindView
import butterknife.ButterKnife
import im.vector.matrix.android.api.session.Session
import im.vector.matrix.android.api.session.crypto.keysbackup.KeysBackupState
import im.vector.matrix.android.internal.crypto.keysbackup.model.KeysBackupVersionTrust
import im.vector.riotredesign.R
import im.vector.riotredesign.core.ui.list.GenericItemViewHolder
import im.vector.riotredesign.core.ui.list.GenericRecyclerViewItem

class KeysBackupSettingsRecyclerViewAdapter(val context: Context) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    val inflater: LayoutInflater = LayoutInflater.from(context)

    private var infoList: List<GenericRecyclerViewItem> = ArrayList()

    private var isBackupAlreadySetup = false

    var adapterListener: AdapterListener? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            GenericItemViewHolder.resId -> GenericItemViewHolder(inflater.inflate(viewType, parent, false))
            else -> FooterViewHolder(inflater.inflate(viewType, parent, false))
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (position < infoList.size) {
            GenericItemViewHolder.resId
        } else {
            R.layout.item_keys_backup_settings_button_footer
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is GenericItemViewHolder) {
            holder.bind(infoList[position])
        } else if (holder is FooterViewHolder) {
            if (isBackupAlreadySetup) {
                holder.button1.setText(R.string.keys_backup_settings_restore_backup_button)
                holder.button1.isVisible = true
                holder.button1.setOnClickListener {
                    adapterListener?.didSelectRestoreMessageRecovery()
                }

                holder.button2.setText(R.string.keys_backup_settings_delete_backup_button)
                holder.button2.isVisible = true
                holder.button2.setOnClickListener {
                    adapterListener?.didSelectDeleteSetupMessageRecovery()
                }
            } else {
                holder.button1.setText(R.string.keys_backup_setup)
                holder.button1.isVisible = true
                holder.button1.setOnClickListener {
                    adapterListener?.didSelectSetupMessageRecovery()
                }

                holder.button2.isVisible = false
            }
        }
    }

    override fun getItemCount(): Int {
        return infoList.size + 1 /*footer*/
    }


    fun updateWithTrust(session: Session, keyBackupVersionTrust: KeysBackupVersionTrust?) {
        val keyBackupState = session.getKeysBackupService().state
        val keyVersionResult = session.getKeysBackupService().mKeysBackupVersion

        val infos = ArrayList<GenericRecyclerViewItem>()
        var itemSummary: GenericRecyclerViewItem? = null

        when (keyBackupState) {
            KeysBackupState.Unknown,
            KeysBackupState.CheckingBackUpOnHomeserver -> {
                //In this cases recycler view is hidden any way
                //so do nothing
            }
            KeysBackupState.Disabled -> {
                itemSummary = GenericRecyclerViewItem(context.getString(R.string.keys_backup_settings_status_not_setup),
                        style = GenericRecyclerViewItem.STYLE.BIG_TEXT)

                isBackupAlreadySetup = false
            }
            KeysBackupState.WrongBackUpVersion,
            KeysBackupState.NotTrusted,
            KeysBackupState.Enabling -> {
                itemSummary = GenericRecyclerViewItem(context.getString(R.string.keys_backup_settings_status_ko),
                        style = GenericRecyclerViewItem.STYLE.BIG_TEXT).apply {
                    description = keyBackupState.toString()
                    endIconResourceId = R.drawable.unit_test_ko
                }

                isBackupAlreadySetup = true
            }
            KeysBackupState.ReadyToBackUp -> {
                itemSummary = GenericRecyclerViewItem(context.getString(R.string.keys_backup_settings_status_ok),
                        style = GenericRecyclerViewItem.STYLE.BIG_TEXT).apply {
                    endIconResourceId = R.drawable.unit_test_ok
                    description = context.getString(R.string.keys_backup_info_keys_all_backup_up)
                }

                isBackupAlreadySetup = true
            }
            KeysBackupState.WillBackUp,
            KeysBackupState.BackingUp -> {
                itemSummary = GenericRecyclerViewItem(context.getString(R.string.keys_backup_settings_status_ok),
                        style = GenericRecyclerViewItem.STYLE.BIG_TEXT).apply {
                    hasIndeterminateProcess = true

                    val totalKeys = session.inboundGroupSessionsCount(false)
                            ?: 0
                    val backedUpKeys = session.inboundGroupSessionsCount(true)
                            ?: 0

                    val remainingKeysToBackup = totalKeys - backedUpKeys

                    description = context.resources.getQuantityString(R.plurals.keys_backup_info_keys_backing_up, remainingKeysToBackup, remainingKeysToBackup)
                }

                isBackupAlreadySetup = true
            }
        }

        itemSummary?.let {
            infos.add(it)
        }

        if (keyBackupVersionTrust != null) {

            if (!keyBackupVersionTrust.usable) {
                itemSummary?.description = context.getString(R.string.keys_backup_settings_untrusted_backup)
            }

            //Add infos
            infos.add(GenericRecyclerViewItem(context.getString(R.string.keys_backup_info_title_version), keyVersionResult?.version
                    ?: ""))
            infos.add(GenericRecyclerViewItem(context.getString(R.string.keys_backup_info_title_algorithm), keyVersionResult?.algorithm
                    ?: ""))

            keyBackupVersionTrust.signatures.forEach {
                val signatureInfo = GenericRecyclerViewItem(context.getString(R.string.keys_backup_info_title_signature))
                val isDeviceKnown = it.device != null
                val isDeviceVerified = it.device?.isVerified ?: false
                val isSignatureValid = it.valid
                val deviceId: String = it.deviceId ?: ""

                if (!isDeviceKnown) {
                    signatureInfo.description = context.getString(R.string.keys_backup_settings_signature_from_unknown_device, deviceId)
                    signatureInfo.endIconResourceId = R.drawable.e2e_warning
                } else {
                    if (isSignatureValid) {
                        if (session.sessionParams.credentials.deviceId == it.deviceId) {
                            signatureInfo.description = context.getString(R.string.keys_backup_settings_valid_signature_from_this_device)
                            signatureInfo.endIconResourceId = R.drawable.e2e_verified
                        } else {
                            if (isDeviceVerified) {
                                signatureInfo.description = context.getString(R.string.keys_backup_settings_valid_signature_from_verified_device, deviceId)
                                signatureInfo.endIconResourceId = R.drawable.e2e_verified
                            } else {
                                signatureInfo.description = context.getString(R.string.keys_backup_settings_valid_signature_from_unverified_device, deviceId)
                                signatureInfo.endIconResourceId = R.drawable.e2e_warning
                            }
                        }
                    } else {
                        //Invalid signature
                        signatureInfo.endIconResourceId = R.drawable.e2e_warning
                        if (isDeviceVerified) {
                            signatureInfo.description = context.getString(R.string.keys_backup_settings_invalid_signature_from_verified_device, deviceId)
                        } else {
                            signatureInfo.description = context.getString(R.string.keys_backup_settings_invalid_signature_from_unverified_device, deviceId)
                        }
                    }
                }

                infos.add(signatureInfo)
            } //end for each
        }

        infoList = infos

        notifyDataSetChanged()
    }

    class FooterViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        init {
            ButterKnife.bind(this, itemView)
        }

        @BindView(R.id.keys_backup_settings_footer_button1)
        lateinit var button1: Button

        @BindView(R.id.keys_backup_settings_footer_button2)
        lateinit var button2: Button

        fun bind() {

        }
    }

    interface AdapterListener {
        fun didSelectSetupMessageRecovery()
        fun didSelectRestoreMessageRecovery()
        fun didSelectDeleteSetupMessageRecovery()
    }

}