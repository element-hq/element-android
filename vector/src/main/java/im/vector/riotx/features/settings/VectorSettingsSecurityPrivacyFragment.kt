/*
 * Copyright 2019 New Vector Ltd
 * Copyright 2020 New Vector Ltd
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

package im.vector.riotx.features.settings

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.SwitchPreference
import com.google.android.material.textfield.TextInputEditText
import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.internal.crypto.crosssigning.isVerified
import im.vector.matrix.android.internal.crypto.model.ImportRoomKeysResult
import im.vector.matrix.android.internal.crypto.model.rest.DeviceInfo
import im.vector.matrix.android.internal.crypto.model.rest.DevicesListResponse
import im.vector.riotx.R
import im.vector.riotx.core.dialogs.ExportKeysDialog
import im.vector.riotx.core.intent.ExternalIntentData
import im.vector.riotx.core.intent.analyseIntent
import im.vector.riotx.core.intent.getFilenameFromUri
import im.vector.riotx.core.platform.SimpleTextWatcher
import im.vector.riotx.core.preference.VectorPreference
import im.vector.riotx.core.utils.PERMISSIONS_FOR_WRITING_FILES
import im.vector.riotx.core.utils.PERMISSION_REQUEST_CODE_EXPORT_KEYS
import im.vector.riotx.core.utils.allGranted
import im.vector.riotx.core.utils.checkPermissions
import im.vector.riotx.core.utils.openFileSelection
import im.vector.riotx.core.utils.toast
import im.vector.riotx.features.crypto.keys.KeysExporter
import im.vector.riotx.features.crypto.keys.KeysImporter
import im.vector.riotx.features.crypto.keysbackup.settings.KeysBackupManageActivity
import javax.inject.Inject

class VectorSettingsSecurityPrivacyFragment @Inject constructor(
        private val vectorPreferences: VectorPreferences
) : VectorSettingsBaseFragment() {

    override var titleRes = R.string.settings_security_and_privacy
    override val preferenceXmlRes = R.xml.vector_settings_security_privacy

    // cryptography
    private val mCryptographyCategory by lazy {
        findPreference<PreferenceCategory>(VectorPreferences.SETTINGS_CRYPTOGRAPHY_PREFERENCE_KEY)!!
    }

    private val mCrossSigningStatePreference by lazy {
        findPreference<VectorPreference>(VectorPreferences.SETTINGS_ENCRYPTION_CROSS_SIGNING_PREFERENCE_KEY)!!
    }

    private val manageBackupPref by lazy {
        findPreference<VectorPreference>(VectorPreferences.SETTINGS_SECURE_MESSAGE_RECOVERY_PREFERENCE_KEY)!!
    }

    private val exportPref by lazy {
        findPreference<VectorPreference>(VectorPreferences.SETTINGS_ENCRYPTION_EXPORT_E2E_ROOM_KEYS_PREFERENCE_KEY)!!
    }

    private val importPref by lazy {
        findPreference<VectorPreference>(VectorPreferences.SETTINGS_ENCRYPTION_IMPORT_E2E_ROOM_KEYS_PREFERENCE_KEY)!!
    }

    private val showDeviceListPref by lazy {
        findPreference<VectorPreference>(VectorPreferences.SETTINGS_SHOW_DEVICES_LIST_PREFERENCE_KEY)!!
    }

    // encrypt to unverified devices
    private val sendToUnverifiedDevicesPref by lazy {
        findPreference<SwitchPreference>(VectorPreferences.SETTINGS_ENCRYPTION_NEVER_SENT_TO_PREFERENCE_KEY)!!
    }

    override fun onResume() {
        super.onResume()
        // My device name may have been updated
        refreshMyDevice()
        refreshXSigningStatus()
    }

    override fun bindPref() {
        // Push target
        refreshPushersList()

        // Refresh Key Management section
        refreshKeysManagementSection()

        // Analytics

        // Analytics tracking management
        findPreference<SwitchPreference>(VectorPreferences.SETTINGS_USE_ANALYTICS_KEY)!!.let {
            // On if the analytics tracking is activated
            it.isChecked = vectorPreferences.useAnalytics()

            it.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
                vectorPreferences.setUseAnalytics(newValue as Boolean)
                true
            }
        }

        refreshXSigningStatus()
    }

    private fun refreshXSigningStatus() {
            val xSigningIsEnableInAccount = session.cryptoService().crossSigningService().isCrossSigningInitialized()
            val xSigningKeysAreTrusted = session.cryptoService().crossSigningService().checkUserTrust(session.myUserId).isVerified()
            val xSigningKeyCanSign = session.cryptoService().crossSigningService().canCrossSign()

            if (xSigningKeyCanSign) {
                mCrossSigningStatePreference.setIcon(R.drawable.ic_shield_trusted)
                mCrossSigningStatePreference.summary = getString(R.string.encryption_information_dg_xsigning_complete)
            } else if (xSigningKeysAreTrusted) {
                mCrossSigningStatePreference.setIcon(R.drawable.ic_shield_custom)
                mCrossSigningStatePreference.summary = getString(R.string.encryption_information_dg_xsigning_trusted)
            } else if (xSigningIsEnableInAccount) {
                mCrossSigningStatePreference.setIcon(R.drawable.ic_shield_black)
                mCrossSigningStatePreference.summary = getString(R.string.encryption_information_dg_xsigning_not_trusted)
            } else {
                mCrossSigningStatePreference.setIcon(android.R.color.transparent)
                mCrossSigningStatePreference.summary = getString(R.string.encryption_information_dg_xsigning_disabled)
            }

            mCrossSigningStatePreference.isVisible = true
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (allGranted(grantResults)) {
            if (requestCode == PERMISSION_REQUEST_CODE_EXPORT_KEYS) {
                exportKeys()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                REQUEST_E2E_FILE_REQUEST_CODE -> importKeys(data)
            }
        }
    }

    private fun refreshKeysManagementSection() {
        // If crypto is not enabled parent section will be removed
        // TODO notice that this will not work when no network
        manageBackupPref.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            context?.let {
                startActivity(KeysBackupManageActivity.intent(it))
            }
            false
        }

        exportPref.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            exportKeys()
            true
        }

        importPref.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            importKeys()
            true
        }
    }

    /**
     * Manage the e2e keys export.
     */
    private fun exportKeys() {
        // We need WRITE_EXTERNAL permission
        if (checkPermissions(PERMISSIONS_FOR_WRITING_FILES,
                        this,
                        PERMISSION_REQUEST_CODE_EXPORT_KEYS,
                        R.string.permissions_rationale_msg_keys_backup_export)) {
            activity?.let { activity ->
                ExportKeysDialog().show(activity, object : ExportKeysDialog.ExportKeyDialogListener {
                    override fun onPassphrase(passphrase: String) {
                        displayLoadingView()

                        KeysExporter(session)
                                .export(requireContext(),
                                        passphrase,
                                        object : MatrixCallback<String> {
                                            override fun onSuccess(data: String) {
                                                if (isAdded) {
                                                    hideLoadingView()

                                                    AlertDialog.Builder(activity)
                                                            .setMessage(getString(R.string.encryption_export_saved_as, data))
                                                            .setCancelable(false)
                                                            .setPositiveButton(R.string.ok, null)
                                                            .show()
                                                }
                                            }

                                            override fun onFailure(failure: Throwable) {
                                                onCommonDone(failure.localizedMessage)
                                            }
                                        })
                    }
                })
            }
        }
    }

    /**
     * Manage the e2e keys import.
     */
    @SuppressLint("NewApi")
    private fun importKeys() {
        activity?.let { openFileSelection(it, this, false, REQUEST_E2E_FILE_REQUEST_CODE) }
    }

    /**
     * Manage the e2e keys import.
     *
     * @param intent the intent result
     */
    private fun importKeys(intent: Intent?) {
        // sanity check
        if (null == intent) {
            return
        }

        val sharedDataItems = analyseIntent(intent)
        val thisActivity = activity

        if (sharedDataItems.isNotEmpty() && thisActivity != null) {
            val sharedDataItem = sharedDataItems[0]

            val uri = when (sharedDataItem) {
                is ExternalIntentData.IntentDataUri      -> sharedDataItem.uri
                is ExternalIntentData.IntentDataClipData -> sharedDataItem.clipDataItem.uri
                else                                     -> null
            }

            val mimetype = when (sharedDataItem) {
                is ExternalIntentData.IntentDataClipData -> sharedDataItem.mimeType
                else                                     -> null
            }

            if (uri == null) {
                return
            }

            val appContext = thisActivity.applicationContext

            val filename = getFilenameFromUri(appContext, uri)

            val dialogLayout = thisActivity.layoutInflater.inflate(R.layout.dialog_import_e2e_keys, null)

            val textView = dialogLayout.findViewById<TextView>(R.id.dialog_e2e_keys_passphrase_filename)

            if (filename.isNullOrBlank()) {
                textView.isVisible = false
            } else {
                textView.isVisible = true
                textView.text = getString(R.string.import_e2e_keys_from_file, filename)
            }

            val builder = AlertDialog.Builder(thisActivity)
                    .setTitle(R.string.encryption_import_room_keys)
                    .setView(dialogLayout)

            val passPhraseEditText = dialogLayout.findViewById<TextInputEditText>(R.id.dialog_e2e_keys_passphrase_edit_text)
            val importButton = dialogLayout.findViewById<Button>(R.id.dialog_e2e_keys_import_button)

            passPhraseEditText.addTextChangedListener(object : SimpleTextWatcher() {
                override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                    importButton.isEnabled = !passPhraseEditText.text.isNullOrEmpty()
                }
            })

            val importDialog = builder.show()

            importButton.setOnClickListener {
                val password = passPhraseEditText.text.toString()

                displayLoadingView()

                KeysImporter(session)
                        .import(requireContext(),
                                uri,
                                mimetype,
                                password,
                                object : MatrixCallback<ImportRoomKeysResult> {
                                    override fun onSuccess(data: ImportRoomKeysResult) {
                                        if (!isAdded) {
                                            return
                                        }

                                        hideLoadingView()

                                        AlertDialog.Builder(thisActivity)
                                                .setMessage(getString(R.string.encryption_import_room_keys_success,
                                                        data.successfullyNumberOfImportedKeys,
                                                        data.totalNumberOfKeys))
                                                .setPositiveButton(R.string.ok) { dialog, _ -> dialog.dismiss() }
                                                .show()
                                    }

                                    override fun onFailure(failure: Throwable) {
                                        appContext.toast(failure.localizedMessage ?: getString(R.string.unexpected_error))
                                        hideLoadingView()
                                    }
                                })

                importDialog.dismiss()
            }
        }
    }

    // ==============================================================================================================
    // Cryptography
    // ==============================================================================================================

    /**
     * Build the cryptography preference section.
     *
     * @param aMyDeviceInfo the device info
     */
    private fun refreshCryptographyPreference(devices: List<DeviceInfo>) {
        showDeviceListPref.isEnabled = devices.isNotEmpty()
        showDeviceListPref.summary = resources.getQuantityString(R.plurals.settings_active_sessions_count, devices.size, devices.size)
//        val userId = session.myUserId
//        val deviceId = session.sessionParams.deviceId

        // device name
//        if (null != aMyDeviceInfo) {
//            cryptoInfoDeviceNamePreference.summary = aMyDeviceInfo.displayName
//
//            cryptoInfoDeviceNamePreference.onPreferenceClickListener = Preference.OnPreferenceClickListener {
//                // TODO device can be rename only from the device list screen for the moment
//                // displayDeviceRenameDialog(aMyDeviceInfo)
//                true
//            }
//
//            cryptoInfoDeviceNamePreference.onPreferenceLongClickListener = object : VectorPreference.OnPreferenceLongClickListener {
//                override fun onPreferenceLongClick(preference: Preference): Boolean {
//                    activity?.let { copyToClipboard(it, aMyDeviceInfo.displayName!!) }
//                    return true
//                }
//            }
//        }
//
//        // crypto section: device ID
//        if (!deviceId.isNullOrEmpty()) {
//            cryptoInfoDeviceIdPreference.summary = deviceId
//
//            cryptoInfoDeviceIdPreference.setOnPreferenceClickListener {
//                activity?.let { copyToClipboard(it, deviceId) }
//                true
//            }
//        }
//
//        // crypto section: device key (fingerprint)
//        if (!deviceId.isNullOrEmpty() && userId.isNotEmpty()) {
//            val deviceInfo = session.getDeviceInfo(userId, deviceId)
//
//            if (null != deviceInfo && !deviceInfo.fingerprint().isNullOrEmpty()) {
//                cryptoInfoTextPreference.summary = deviceInfo.getFingerprintHumanReadable()
//
//                cryptoInfoTextPreference.setOnPreferenceClickListener {
//                    deviceInfo.fingerprint()?.let {
//                        copyToClipboard(requireActivity(), it)
//                    }
//                    true
//                }
//            }
//        }

        sendToUnverifiedDevicesPref.isChecked = false

        sendToUnverifiedDevicesPref.isChecked = session.cryptoService().getGlobalBlacklistUnverifiedDevices()

        sendToUnverifiedDevicesPref.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            session.cryptoService().setGlobalBlacklistUnverifiedDevices(sendToUnverifiedDevicesPref.isChecked)

            true
        }
    }

    // ==============================================================================================================
    // devices list
    // ==============================================================================================================

    private fun refreshMyDevice() {
        session.cryptoService().getUserDevices(session.myUserId).map {
            DeviceInfo(
                    user_id = session.myUserId,
                    deviceId = it.deviceId,
                    displayName = it.displayName()
            )
        }.let {
            refreshCryptographyPreference(it)
        }
        // TODO Move to a ViewModel...
        session.cryptoService().fetchDevicesList(object : MatrixCallback<DevicesListResponse> {
            override fun onSuccess(data: DevicesListResponse) {
                if (isAdded) {
                    refreshCryptographyPreference(data.devices.orEmpty())
                }
            }

            override fun onFailure(failure: Throwable) {
                if (isAdded) {
                    refreshCryptographyPreference(emptyList())
                }
            }
        })
    }

    // ==============================================================================================================
    // pushers list management
    // ==============================================================================================================

    /**
     * Refresh the pushers list
     */
    private fun refreshPushersList() {
        activity?.let { _ ->
            /* TODO
            val pushManager = Matrix.getInstance(activity).pushManager
            val pushersList = ArrayList(pushManager.mPushersList)

            if (pushersList.isEmpty()) {
                preferenceScreen.removePreference(mPushersSettingsCategory)
                preferenceScreen.removePreference(mPushersSettingsDivider)
                return
            }

            // check first if there is an update
            var isNewList = true
            if (pushersList.size == mDisplayedPushers.size) {
                isNewList = !mDisplayedPushers.containsAll(pushersList)
            }

            if (isNewList) {
                // remove the displayed one
                mPushersSettingsCategory.removeAll()

                // add new emails list
                mDisplayedPushers = pushersList

                var index = 0

                for (pushRule in mDisplayedPushers) {
                    if (null != pushRule.lang) {
                        val isThisDeviceTarget = TextUtils.equals(pushManager.currentRegistrationToken, pushRule.pushkey)

                        val preference = VectorPreference(activity).apply {
                            mTypeface = if (isThisDeviceTarget) Typeface.BOLD else Typeface.NORMAL
                        }
                        preference.title = pushRule.deviceDisplayName
                        preference.summary = pushRule.appDisplayName
                        preference.key = PUSHER_PREFERENCE_KEY_BASE + index
                        index++
                        mPushersSettingsCategory.addPreference(preference)

                        // the user cannot remove the self device target
                        if (!isThisDeviceTarget) {
                            preference.onPreferenceLongClickListener = object : VectorPreference.OnPreferenceLongClickListener {
                                override fun onPreferenceLongClick(preference: Preference): Boolean {
                                    AlertDialog.Builder(activity)
                                            .setTitle(R.string.dialog_title_confirmation)
                                            .setMessage(R.string.settings_delete_notification_targets_confirmation)
                                            .setPositiveButton(R.string.remove)
                                            { _, _ ->
                                                displayLoadingView()
                                                pushManager.unregister(session, pushRule, object : MatrixCallback<Unit> {
                                                    override fun onSuccess(info: Void?) {
                                                        refreshPushersList()
                                                        onCommonDone(null)
                                                    }

                                                    override fun onNetworkError(e: Exception) {
                                                        onCommonDone(e.localizedMessage)
                                                    }

                                                    override fun onMatrixError(e: MatrixError) {
                                                        onCommonDone(e.localizedMessage)
                                                    }

                                                    override fun onUnexpectedError(e: Exception) {
                                                        onCommonDone(e.localizedMessage)
                                                    }
                                                })
                                            }
                                            .setNegativeButton(R.string.cancel, null)
                                            .show()
                                    return true
                                }
                            }
                        }
                    }
                }
            }
        */
        }
    }

    companion object {
        private const val REQUEST_E2E_FILE_REQUEST_CODE = 123

        private const val PUSHER_PREFERENCE_KEY_BASE = "PUSHER_PREFERENCE_KEY_BASE"
        private const val DEVICES_PREFERENCE_KEY_BASE = "DEVICES_PREFERENCE_KEY_BASE"

        // TODO i18n
        const val LABEL_UNAVAILABLE_DATA = "none"
    }
}
