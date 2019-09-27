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

package im.vector.riotx.features.settings

import android.annotation.SuppressLint
import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Typeface
import android.text.TextUtils
import android.view.KeyEvent
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.SwitchPreference
import com.google.android.material.textfield.TextInputEditText
import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.api.extensions.getFingerprintHumanReadable
import im.vector.matrix.android.api.extensions.sortByLastSeen
import im.vector.matrix.android.api.failure.Failure
import im.vector.matrix.android.internal.auth.data.LoginFlowTypes
import im.vector.matrix.android.internal.crypto.model.ImportRoomKeysResult
import im.vector.matrix.android.internal.crypto.model.rest.DeviceInfo
import im.vector.matrix.android.internal.crypto.model.rest.DevicesListResponse
import im.vector.riotx.R
import im.vector.riotx.core.di.ScreenComponent
import im.vector.riotx.core.dialogs.ExportKeysDialog
import im.vector.riotx.core.intent.ExternalIntentData
import im.vector.riotx.core.intent.analyseIntent
import im.vector.riotx.core.intent.getFilenameFromUri
import im.vector.riotx.core.platform.SimpleTextWatcher
import im.vector.riotx.core.preference.ProgressBarPreference
import im.vector.riotx.core.preference.VectorPreference
import im.vector.riotx.core.preference.VectorPreferenceDivider
import im.vector.riotx.core.utils.*
import im.vector.riotx.features.crypto.keys.KeysExporter
import im.vector.riotx.features.crypto.keys.KeysImporter
import im.vector.riotx.features.crypto.keysbackup.settings.KeysBackupManageActivity
import timber.log.Timber
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

class VectorSettingsSecurityPrivacyFragment : VectorSettingsBaseFragment() {

    override var titleRes = R.string.settings_security_and_privacy
    override val preferenceXmlRes = R.xml.vector_settings_security_privacy

    // used to avoid requesting to enter the password for each deletion
    private var mAccountPassword: String = ""

    // devices: device IDs and device names
    private var mDevicesNameList: List<DeviceInfo> = ArrayList()

    private var mMyDeviceInfo: DeviceInfo? = null


    // cryptography
    private val mCryptographyCategory by lazy {
        findPreference<PreferenceCategory>(VectorPreferences.SETTINGS_CRYPTOGRAPHY_PREFERENCE_KEY)!!
    }
    private val mCryptographyCategoryDivider by lazy {
        findPreference<VectorPreferenceDivider>(VectorPreferences.SETTINGS_CRYPTOGRAPHY_DIVIDER_PREFERENCE_KEY)!!
    }
    // cryptography manage
    private val mCryptographyManageCategory by lazy {
        findPreference<PreferenceCategory>(VectorPreferences.SETTINGS_CRYPTOGRAPHY_MANAGE_PREFERENCE_KEY)!!
    }
    private val mCryptographyManageCategoryDivider by lazy {
        findPreference<VectorPreferenceDivider>(VectorPreferences.SETTINGS_CRYPTOGRAPHY_MANAGE_DIVIDER_PREFERENCE_KEY)!!
    }
    // displayed pushers
    private val mPushersSettingsDivider by lazy {
        findPreference<VectorPreferenceDivider>(VectorPreferences.SETTINGS_NOTIFICATIONS_TARGET_DIVIDER_PREFERENCE_KEY)!!
    }
    private val mPushersSettingsCategory by lazy {
        findPreference<PreferenceCategory>(VectorPreferences.SETTINGS_NOTIFICATIONS_TARGETS_PREFERENCE_KEY)!!
    }
    private val mDevicesListSettingsCategory by lazy {
        findPreference<PreferenceCategory>(VectorPreferences.SETTINGS_DEVICES_LIST_PREFERENCE_KEY)!!
    }
    private val mDevicesListSettingsCategoryDivider by lazy {
        findPreference<VectorPreferenceDivider>(VectorPreferences.SETTINGS_DEVICES_DIVIDER_PREFERENCE_KEY)!!
    }
    private val cryptoInfoDeviceNamePreference by lazy {
        findPreference<VectorPreference>(VectorPreferences.SETTINGS_ENCRYPTION_INFORMATION_DEVICE_NAME_PREFERENCE_KEY)!!
    }

    private val cryptoInfoDeviceIdPreference by lazy {
        findPreference<VectorPreference>(VectorPreferences.SETTINGS_ENCRYPTION_INFORMATION_DEVICE_ID_PREFERENCE_KEY)!!
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

    private val cryptoInfoTextPreference by lazy {
        findPreference<VectorPreference>(VectorPreferences.SETTINGS_ENCRYPTION_INFORMATION_DEVICE_KEY_PREFERENCE_KEY)!!
    }
    // encrypt to unverified devices
    private val sendToUnverifiedDevicesPref by lazy {
        findPreference<SwitchPreference>(VectorPreferences.SETTINGS_ENCRYPTION_NEVER_SENT_TO_PREFERENCE_KEY)!!
    }

    @Inject lateinit var vectorPreferences: VectorPreferences

    override fun injectWith(injector: ScreenComponent) {
        injector.inject(this)
    }

    override fun bindPref() {
        // Push target
        refreshPushersList()

        // Device list
        refreshDevicesList()

        //Refresh Key Management section
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

        // Rageshake Management
        findPreference<SwitchPreference>(VectorPreferences.SETTINGS_USE_RAGE_SHAKE_KEY)!!.let {
            it.isChecked = vectorPreferences.useRageshake()

            it.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
                vectorPreferences.setUseRageshake(newValue as Boolean)
                true
            }
        }
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
        //If crypto is not enabled parent section will be removed
        //TODO notice that this will not work when no network
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
        if (checkPermissions(PERMISSIONS_FOR_WRITING_FILES, this, PERMISSION_REQUEST_CODE_EXPORT_KEYS)) {
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
                    importButton.isEnabled = !TextUtils.isEmpty(passPhraseEditText.text)
                }
            })

            val importDialog = builder.show()

            importButton.setOnClickListener(View.OnClickListener {
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
                                        appContext.toast(failure.localizedMessage)
                                        hideLoadingView()
                                    }
                                })

                importDialog.dismiss()
            })
        }
    }

    //==============================================================================================================
    // Cryptography
    //==============================================================================================================

    private fun removeCryptographyPreference() {
        preferenceScreen.let {
            it.removePreference(mCryptographyCategory)
            it.removePreference(mCryptographyCategoryDivider)

            // Also remove keys management section
            it.removePreference(mCryptographyManageCategory)
            it.removePreference(mCryptographyManageCategoryDivider)
        }
    }

    /**
     * Build the cryptography preference section.
     *
     * @param aMyDeviceInfo the device info
     */
    private fun refreshCryptographyPreference(aMyDeviceInfo: DeviceInfo?) {
        val userId = session.myUserId
        val deviceId = session.sessionParams.credentials.deviceId

        // device name
        if (null != aMyDeviceInfo) {
            cryptoInfoDeviceNamePreference.summary = aMyDeviceInfo.displayName

            cryptoInfoDeviceNamePreference.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                displayDeviceRenameDialog(aMyDeviceInfo)
                true
            }

            cryptoInfoDeviceNamePreference.onPreferenceLongClickListener = object : VectorPreference.OnPreferenceLongClickListener {
                override fun onPreferenceLongClick(preference: Preference): Boolean {
                    activity?.let { copyToClipboard(it, aMyDeviceInfo.displayName!!) }
                    return true
                }
            }
        }

        // crypto section: device ID
        if (!TextUtils.isEmpty(deviceId)) {
            cryptoInfoDeviceIdPreference.summary = deviceId

            cryptoInfoDeviceIdPreference.setOnPreferenceClickListener {
                activity?.let { copyToClipboard(it, deviceId!!) }
                true
            }
        }

        // crypto section: device key (fingerprint)
        if (!TextUtils.isEmpty(deviceId) && !TextUtils.isEmpty(userId)) {
            val deviceInfo = session.getDeviceInfo(userId, deviceId)

            if (null != deviceInfo && !TextUtils.isEmpty(deviceInfo.fingerprint())) {
                cryptoInfoTextPreference.summary = deviceInfo.getFingerprintHumanReadable()

                cryptoInfoTextPreference.setOnPreferenceClickListener {
                    deviceInfo.fingerprint()?.let {
                        copyToClipboard(requireActivity(), it)
                    }
                    true
                }
            }
        }

        sendToUnverifiedDevicesPref.isChecked = false

        sendToUnverifiedDevicesPref.isChecked = session.getGlobalBlacklistUnverifiedDevices()

        sendToUnverifiedDevicesPref.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            session.setGlobalBlacklistUnverifiedDevices(sendToUnverifiedDevicesPref.isChecked)

            true
        }
    }


    //==============================================================================================================
    // devices list
    //==============================================================================================================

    private fun removeDevicesPreference() {
        preferenceScreen.let {
            it.removePreference(mDevicesListSettingsCategory)
            it.removePreference(mDevicesListSettingsCategoryDivider)
        }
    }

    /**
     * Force the refresh of the devices list.<br></br>
     * The devices list is the list of the devices where the user as looged in.
     * It can be any mobile device, as any browser.
     */
    private fun refreshDevicesList() {
        if (session.isCryptoEnabled() && !TextUtils.isEmpty(session.sessionParams.credentials.deviceId)) {
            // display a spinner while loading the devices list
            if (0 == mDevicesListSettingsCategory.preferenceCount) {
                activity?.let {
                    val preference = ProgressBarPreference(it)
                    mDevicesListSettingsCategory.addPreference(preference)
                }
            }

            session.getDevicesList(object : MatrixCallback<DevicesListResponse> {
                override fun onSuccess(data: DevicesListResponse) {
                    if (!isAdded) {
                        return
                    }

                    if (data.devices?.isEmpty() == true) {
                        removeDevicesPreference()
                    } else {
                        buildDevicesSettings(data.devices!!)
                    }
                }

                override fun onFailure(failure: Throwable) {
                    if (!isAdded) {
                        return
                    }

                    removeDevicesPreference()
                    onCommonDone(failure.message)
                }
            })
        } else {
            removeDevicesPreference()
            removeCryptographyPreference()
        }
    }

    /**
     * Build the devices portion of the settings.<br></br>
     * Each row correspond to a device ID and its corresponding device name. Clicking on the row
     * display a dialog containing: the device ID, the device name and the "last seen" information.
     *
     * @param aDeviceInfoList the list of the devices
     */
    private fun buildDevicesSettings(aDeviceInfoList: List<DeviceInfo>) {
        var preference: VectorPreference
        var typeFaceHighlight: Int
        var isNewList = true
        val myDeviceId = session.sessionParams.credentials.deviceId

        if (aDeviceInfoList.size == mDevicesNameList.size) {
            isNewList = !mDevicesNameList.containsAll(aDeviceInfoList)
        }

        if (isNewList) {
            var prefIndex = 0
            mDevicesNameList = aDeviceInfoList

            // sort before display: most recent first
            mDevicesNameList.sortByLastSeen()

            // start from scratch: remove the displayed ones
            mDevicesListSettingsCategory.removeAll()

            for (deviceInfo in mDevicesNameList) {
                // set bold to distinguish current device ID
                if (null != myDeviceId && myDeviceId == deviceInfo.deviceId) {
                    mMyDeviceInfo = deviceInfo
                    typeFaceHighlight = Typeface.BOLD
                } else {
                    typeFaceHighlight = Typeface.NORMAL
                }

                // add the edit text preference
                preference = VectorPreference(requireActivity()).apply {
                    mTypeface = typeFaceHighlight
                }

                if (null == deviceInfo.deviceId && null == deviceInfo.displayName) {
                    continue
                } else {
                    if (null != deviceInfo.deviceId) {
                        preference.title = deviceInfo.deviceId
                    }

                    // display name parameter can be null (new JSON API)
                    if (null != deviceInfo.displayName) {
                        preference.summary = deviceInfo.displayName
                    }
                }

                preference.key = DEVICES_PREFERENCE_KEY_BASE + prefIndex
                prefIndex++

                // onClick handler: display device details dialog
                preference.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                    displayDeviceDetailsDialog(deviceInfo)
                    true
                }

                mDevicesListSettingsCategory.addPreference(preference)
            }

            refreshCryptographyPreference(mMyDeviceInfo)
        }
    }

    /**
     * Display a dialog containing the device ID, the device name and the "last seen" information.<>
     * This dialog allow to delete the corresponding device (see [.displayDeviceDeletionDialog])
     *
     * @param aDeviceInfo the device information
     */
    private fun displayDeviceDetailsDialog(aDeviceInfo: DeviceInfo) {

        activity?.let {

            val builder = AlertDialog.Builder(it)
            val inflater = it.layoutInflater
            val layout = inflater.inflate(R.layout.dialog_device_details, null)
            var textView = layout.findViewById<TextView>(R.id.device_id)

            textView.text = aDeviceInfo.deviceId

            // device name
            textView = layout.findViewById(R.id.device_name)
            val displayName = if (TextUtils.isEmpty(aDeviceInfo.displayName)) LABEL_UNAVAILABLE_DATA else aDeviceInfo.displayName
            textView.text = displayName

            // last seen info
            textView = layout.findViewById(R.id.device_last_seen)

            val lastSeenIp = aDeviceInfo.lastSeenIp?.takeIf { ip -> ip.isNotBlank() } ?: "-"

            val lastSeenTime = aDeviceInfo.lastSeenTs?.let { ts ->
                val dateFormatTime = SimpleDateFormat("HH:mm:ss")
                val date = Date(ts)

                val time = dateFormatTime.format(date)
                val dateFormat = DateFormat.getDateInstance(DateFormat.SHORT, Locale.getDefault())

                dateFormat.format(date) + ", " + time
            } ?: "-"

            val lastSeenInfo = getString(R.string.devices_details_last_seen_format, lastSeenIp, lastSeenTime)
            textView.text = lastSeenInfo

            // title & icon
            builder.setTitle(R.string.devices_details_dialog_title)
                    .setIcon(android.R.drawable.ic_dialog_info)
                    .setView(layout)
                    .setPositiveButton(R.string.rename) { _, _ -> displayDeviceRenameDialog(aDeviceInfo) }

            // disable the deletion for our own device
            if (!TextUtils.equals(session.getMyDevice()?.deviceId, aDeviceInfo.deviceId)) {
                builder.setNegativeButton(R.string.delete) { _, _ -> deleteDevice(aDeviceInfo) }
            }

            builder.setNeutralButton(R.string.cancel, null)
                    .setOnKeyListener(DialogInterface.OnKeyListener { dialog, keyCode, event ->
                        if (event.action == KeyEvent.ACTION_UP && keyCode == KeyEvent.KEYCODE_BACK) {
                            dialog.cancel()
                            return@OnKeyListener true
                        }
                        false
                    })
                    .show()
        }
    }

    /**
     * Display an alert dialog to rename a device
     *
     * @param aDeviceInfoToRename device info
     */
    private fun displayDeviceRenameDialog(aDeviceInfoToRename: DeviceInfo) {
        activity?.let {
            val inflater = it.layoutInflater
            val layout = inflater.inflate(R.layout.dialog_base_edit_text, null)

            val input = layout.findViewById<EditText>(R.id.edit_text)
            input.setText(aDeviceInfoToRename.displayName)

            AlertDialog.Builder(it)
                    .setTitle(R.string.devices_details_device_name)
                    .setView(layout)
                    .setPositiveButton(R.string.ok) { _, _ ->
                        displayLoadingView()

                        val newName = input.text.toString()

                        session.setDeviceName(aDeviceInfoToRename.deviceId!!, newName, object : MatrixCallback<Unit> {
                            override fun onSuccess(data: Unit) {
                                hideLoadingView()

                                // search which preference is updated
                                val count = mDevicesListSettingsCategory.preferenceCount

                                for (i in 0 until count) {
                                    val pref = mDevicesListSettingsCategory.getPreference(i)

                                    if (TextUtils.equals(aDeviceInfoToRename.deviceId, pref.title)) {
                                        pref.summary = newName
                                    }
                                }

                                // detect if the updated device is the current account one
                                if (TextUtils.equals(cryptoInfoDeviceIdPreference.summary, aDeviceInfoToRename.deviceId)) {
                                    cryptoInfoDeviceNamePreference.summary = newName
                                }

                                // Also change the display name in aDeviceInfoToRename, in case of multiple renaming
                                aDeviceInfoToRename.displayName = newName
                            }

                            override fun onFailure(failure: Throwable) {
                                onCommonDone(failure.localizedMessage)
                            }
                        })
                    }
                    .setNegativeButton(R.string.cancel, null)
                    .show()
        }
    }

    /**
     * Try to delete a device.
     *
     * @param deviceInfo the device to delete
     */
    private fun deleteDevice(deviceInfo: DeviceInfo) {
        val deviceId = deviceInfo.deviceId
        if (deviceId == null) {
            Timber.e("## displayDeviceDeletionDialog(): sanity check failure")
            return
        }

        displayLoadingView()
        session.deleteDevice(deviceId, object : MatrixCallback<Unit> {
            override fun onSuccess(data: Unit) {
                hideLoadingView()
                // force settings update
                refreshDevicesList()
            }

            override fun onFailure(failure: Throwable) {
                var isPasswordRequestFound = false

                if (failure is Failure.RegistrationFlowError) {
                    // We only support LoginFlowTypes.PASSWORD
                    // Check if we can provide the user password
                    failure.registrationFlowResponse.flows?.forEach { interactiveAuthenticationFlow ->
                        isPasswordRequestFound = isPasswordRequestFound || interactiveAuthenticationFlow.stages?.any { it == LoginFlowTypes.PASSWORD } == true
                    }

                    if (isPasswordRequestFound) {
                        maybeShowDeleteDeviceWithPasswordDialog(deviceId, failure.registrationFlowResponse.session)
                    }

                }

                if (!isPasswordRequestFound) {
                    // LoginFlowTypes.PASSWORD not supported, and this is the only one RiotX supports so far...
                    onCommonDone(failure.localizedMessage)
                }
            }
        })
    }

    /**
     * Show a dialog to ask for user password, or use a previously entered password.
     */
    private fun maybeShowDeleteDeviceWithPasswordDialog(deviceId: String, authSession: String?) {
        if (!TextUtils.isEmpty(mAccountPassword)) {
            deleteDeviceWithPassword(deviceId, authSession, mAccountPassword)
        } else {
            activity?.let {
                val inflater = it.layoutInflater
                val layout = inflater.inflate(R.layout.dialog_device_delete, null)
                val passwordEditText = layout.findViewById<EditText>(R.id.delete_password)

                AlertDialog.Builder(it)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setTitle(R.string.devices_delete_dialog_title)
                        .setView(layout)
                        .setPositiveButton(R.string.devices_delete_submit_button_label, DialogInterface.OnClickListener { _, _ ->
                            if (TextUtils.isEmpty(passwordEditText.toString())) {
                                it.toast(R.string.error_empty_field_your_password)
                                return@OnClickListener
                            }
                            mAccountPassword = passwordEditText.text.toString()
                            deleteDeviceWithPassword(deviceId, authSession, mAccountPassword)
                        })
                        .setNegativeButton(R.string.cancel) { _, _ ->
                            hideLoadingView()
                        }
                        .setOnKeyListener(DialogInterface.OnKeyListener { dialog, keyCode, event ->
                            if (event.action == KeyEvent.ACTION_UP && keyCode == KeyEvent.KEYCODE_BACK) {
                                dialog.cancel()
                                hideLoadingView()
                                return@OnKeyListener true
                            }
                            false
                        })
                        .show()
            }
        }
    }

    private fun deleteDeviceWithPassword(deviceId: String, authSession: String?, accountPassword: String) {
        session.deleteDeviceWithUserPassword(deviceId, authSession, accountPassword, object : MatrixCallback<Unit> {
            override fun onSuccess(data: Unit) {
                hideLoadingView()
                // force settings update
                refreshDevicesList()
            }

            override fun onFailure(failure: Throwable) {
                // Password is maybe not good
                onCommonDone(failure.localizedMessage)
                mAccountPassword = ""
            }
        })
    }

    //==============================================================================================================
    // pushers list management
    //==============================================================================================================

    /**
     * Refresh the pushers list
     */
    private fun refreshPushersList() {
        activity?.let { activity ->
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
        private const val LABEL_UNAVAILABLE_DATA = "none"
    }
}