/*
 * Copyright 2018 New Vector Ltd
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

package im.vector.riotredesign.features.settings

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Typeface
import android.media.RingtoneManager
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.TextUtils
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.view.isVisible
import androidx.preference.*
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import im.vector.matrix.android.api.Matrix
import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.api.extensions.getFingerprintHumanReadable
import im.vector.matrix.android.api.extensions.sortByLastSeen
import im.vector.matrix.android.api.failure.Failure
import im.vector.matrix.android.api.session.Session
import im.vector.matrix.android.internal.auth.data.LoginFlowTypes
import im.vector.matrix.android.internal.crypto.model.ImportRoomKeysResult
import im.vector.matrix.android.internal.crypto.model.rest.DeviceInfo
import im.vector.matrix.android.internal.crypto.model.rest.DevicesListResponse
import im.vector.riotredesign.R
import im.vector.riotredesign.core.dialogs.ExportKeysDialog
import im.vector.riotredesign.core.extensions.showPassword
import im.vector.riotredesign.core.extensions.withArgs
import im.vector.riotredesign.core.intent.ExternalIntentData
import im.vector.riotredesign.core.intent.analyseIntent
import im.vector.riotredesign.core.intent.getFilenameFromUri
import im.vector.riotredesign.core.platform.SimpleTextWatcher
import im.vector.riotredesign.core.platform.VectorPreferenceFragment
import im.vector.riotredesign.core.preference.BingRule
import im.vector.riotredesign.core.preference.ProgressBarPreference
import im.vector.riotredesign.core.preference.UserAvatarPreference
import im.vector.riotredesign.core.preference.VectorPreference
import im.vector.riotredesign.core.utils.*
import im.vector.riotredesign.features.MainActivity
import im.vector.riotredesign.features.configuration.VectorConfiguration
import im.vector.riotredesign.features.crypto.keys.KeysExporter
import im.vector.riotredesign.features.crypto.keys.KeysImporter
import im.vector.riotredesign.features.crypto.keysbackup.settings.KeysBackupManageActivity
import im.vector.riotredesign.features.themes.ThemeUtils
import im.vector.riotredesign.features.version.getVersion
import org.koin.android.ext.android.inject
import timber.log.Timber
import java.lang.ref.WeakReference
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

class VectorSettingsPreferencesFragment : VectorPreferenceFragment(), SharedPreferences.OnSharedPreferenceChangeListener {

    override var titleRes: Int = R.string.title_activity_settings
    // members
    private val mSession by inject<Session>()

    // disable some updates if there is
    // TODO private val mNetworkListener = IMXNetworkEventListener { refreshDisplay() }
    // events listener
    // TODO private val mEventsListener = object : MXEventListener() {
    // TODO     override fun onBingRulesUpdate() {
    // TODO         refreshPreferences()
    // TODO         refreshDisplay()
    // TODO     }

    // TODO     override fun onAccountInfoUpdate(myUser: MyUser) {
    // TODO         // refresh the settings value
    // TODO         PreferenceManager.getDefaultSharedPreferences(VectorApp.getInstance().applicationContext).edit {
    // TODO             putString(PreferencesManager.SETTINGS_DISPLAY_NAME_PREFERENCE_KEY, myUser.displayname)
    // TODO         }

    // TODO         refreshDisplay()
    // TODO     }
    // TODO }

    private var mLoadingView: View? = null

    private var mDisplayedEmails = ArrayList<String>()
    private var mDisplayedPhoneNumber = ArrayList<String>()

    private var mMyDeviceInfo: DeviceInfo? = null

    // TODO private var mDisplayedPushers = ArrayList<Pusher>()

    private var interactionListener: VectorSettingsFragmentInteractionListener? = null

    // devices: device IDs and device names
    private var mDevicesNameList: List<DeviceInfo> = ArrayList()
    // used to avoid requesting to enter the password for each deletion
    private var mAccountPassword: String = ""

    private val vectorConfiguration by inject<VectorConfiguration>()

    // current publicised group list
    private var mPublicisedGroups: MutableSet<String>? = null

    /* ==========================================================================================
     * Preferences
     * ========================================================================================== */

    private val mUserSettingsCategory by lazy {
        findPreference(PreferencesManager.SETTINGS_USER_SETTINGS_PREFERENCE_KEY) as PreferenceCategory
    }
    private val mUserAvatarPreference by lazy {
        findPreference(PreferencesManager.SETTINGS_PROFILE_PICTURE_PREFERENCE_KEY) as UserAvatarPreference
    }
    private val mDisplayNamePreference by lazy {
        findPreference(PreferencesManager.SETTINGS_DISPLAY_NAME_PREFERENCE_KEY) as EditTextPreference
    }
    private val mPasswordPreference by lazy {
        findPreference(PreferencesManager.SETTINGS_CHANGE_PASSWORD_PREFERENCE_KEY)
    }

    // Local contacts
    private val mContactSettingsCategory by lazy {
        findPreference(PreferencesManager.SETTINGS_CONTACT_PREFERENCE_KEYS) as PreferenceCategory
    }

    private val mContactPhonebookCountryPreference by lazy {
        findPreference(PreferencesManager.SETTINGS_CONTACTS_PHONEBOOK_COUNTRY_PREFERENCE_KEY)
    }

    // Group Flairs
    private val mGroupsFlairCategory by lazy {
        findPreference(PreferencesManager.SETTINGS_GROUPS_FLAIR_KEY) as PreferenceCategory
    }

    // cryptography
    private val mCryptographyCategory by lazy {
        findPreference(PreferencesManager.SETTINGS_CRYPTOGRAPHY_PREFERENCE_KEY) as PreferenceCategory
    }
    private val mCryptographyCategoryDivider by lazy {
        findPreference(PreferencesManager.SETTINGS_CRYPTOGRAPHY_DIVIDER_PREFERENCE_KEY)
    }
    // cryptography manage
    private val mCryptographyManageCategory by lazy {
        findPreference(PreferencesManager.SETTINGS_CRYPTOGRAPHY_MANAGE_PREFERENCE_KEY) as PreferenceCategory
    }
    private val mCryptographyManageCategoryDivider by lazy {
        findPreference(PreferencesManager.SETTINGS_CRYPTOGRAPHY_MANAGE_DIVIDER_PREFERENCE_KEY)
    }
    // displayed pushers
    private val mPushersSettingsDivider by lazy {
        findPreference(PreferencesManager.SETTINGS_NOTIFICATIONS_TARGET_DIVIDER_PREFERENCE_KEY)
    }
    private val mPushersSettingsCategory by lazy {
        findPreference(PreferencesManager.SETTINGS_NOTIFICATIONS_TARGETS_PREFERENCE_KEY) as PreferenceCategory
    }
    private val mDevicesListSettingsCategory by lazy {
        findPreference(PreferencesManager.SETTINGS_DEVICES_LIST_PREFERENCE_KEY) as PreferenceCategory
    }
    private val mDevicesListSettingsCategoryDivider by lazy {
        findPreference(PreferencesManager.SETTINGS_DEVICES_DIVIDER_PREFERENCE_KEY)
    }
    // displayed the ignored users list
    private val mIgnoredUserSettingsCategoryDivider by lazy {
        findPreference(PreferencesManager.SETTINGS_IGNORE_USERS_DIVIDER_PREFERENCE_KEY)
    }
    private val mIgnoredUserSettingsCategory by lazy {
        findPreference(PreferencesManager.SETTINGS_IGNORED_USERS_PREFERENCE_KEY) as PreferenceCategory
    }
    // background sync category
    private val mSyncRequestTimeoutPreference by lazy {
        // ? Cause it can be removed
        findPreference(PreferencesManager.SETTINGS_SET_SYNC_TIMEOUT_PREFERENCE_KEY) as EditTextPreference?
    }
    private val mSyncRequestDelayPreference by lazy {
        // ? Cause it can be removed
        findPreference(PreferencesManager.SETTINGS_SET_SYNC_DELAY_PREFERENCE_KEY) as EditTextPreference?
    }
    private val mLabsCategory by lazy {
        findPreference(PreferencesManager.SETTINGS_LABS_PREFERENCE_KEY) as PreferenceCategory
    }
    private val backgroundSyncCategory by lazy {
        findPreference(PreferencesManager.SETTINGS_BACKGROUND_SYNC_PREFERENCE_KEY)
    }
    private val backgroundSyncDivider by lazy {
        findPreference(PreferencesManager.SETTINGS_BACKGROUND_SYNC_DIVIDER_PREFERENCE_KEY)
    }
    private val backgroundSyncPreference by lazy {
        findPreference(PreferencesManager.SETTINGS_ENABLE_BACKGROUND_SYNC_PREFERENCE_KEY) as SwitchPreference
    }
    private val mUseRiotCallRingtonePreference by lazy {
        findPreference(PreferencesManager.SETTINGS_CALL_RINGTONE_USE_RIOT_PREFERENCE_KEY) as SwitchPreference
    }
    private val mCallRingtonePreference by lazy {
        findPreference(PreferencesManager.SETTINGS_CALL_RINGTONE_URI_PREFERENCE_KEY)
    }
    private val notificationsSettingsCategory by lazy {
        findPreference(PreferencesManager.SETTINGS_NOTIFICATIONS_KEY) as PreferenceCategory
    }
    private val mNotificationPrivacyPreference by lazy {
        findPreference(PreferencesManager.SETTINGS_NOTIFICATION_PRIVACY_PREFERENCE_KEY)
    }
    private val selectedLanguagePreference by lazy {
        findPreference(PreferencesManager.SETTINGS_INTERFACE_LANGUAGE_PREFERENCE_KEY)
    }
    private val textSizePreference by lazy {
        findPreference(PreferencesManager.SETTINGS_INTERFACE_TEXT_SIZE_KEY)
    }
    private val cryptoInfoDeviceNamePreference by lazy {
        findPreference(PreferencesManager.SETTINGS_ENCRYPTION_INFORMATION_DEVICE_NAME_PREFERENCE_KEY) as VectorPreference
    }
    private val cryptoInfoDeviceIdPreference by lazy {
        findPreference(PreferencesManager.SETTINGS_ENCRYPTION_INFORMATION_DEVICE_ID_PREFERENCE_KEY)
    }

    private val manageBackupPref by lazy {
        findPreference(PreferencesManager.SETTINGS_SECURE_MESSAGE_RECOVERY_PREFERENCE_KEY)
    }

    private val exportPref by lazy {
        findPreference(PreferencesManager.SETTINGS_ENCRYPTION_EXPORT_E2E_ROOM_KEYS_PREFERENCE_KEY)
    }

    private val importPref by lazy {
        findPreference(PreferencesManager.SETTINGS_ENCRYPTION_IMPORT_E2E_ROOM_KEYS_PREFERENCE_KEY)
    }

    private val cryptoInfoTextPreference by lazy {
        findPreference(PreferencesManager.SETTINGS_ENCRYPTION_INFORMATION_DEVICE_KEY_PREFERENCE_KEY)
    }
    // encrypt to unverified devices
    private val sendToUnverifiedDevicesPref by lazy {
        findPreference(PreferencesManager.SETTINGS_ENCRYPTION_NEVER_SENT_TO_PREFERENCE_KEY) as SwitchPreference
    }

    /* ==========================================================================================
     * Life cycle
     * ========================================================================================== */

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        val appContext = activity?.applicationContext

        // retrieve the arguments
        /*
        val sessionArg = Matrix.getInstance(appContext).getSession(arguments?.getString(ARG_MATRIX_ID))

        // sanity checks
        if (null == sessionArg || !sessionArg.isAlive) {
            activity?.finish()
            return
        }

        session = sessionArg
        */

        // define the layout
        addPreferencesFromResource(R.xml.vector_settings_preferences)

        // Avatar
        mUserAvatarPreference.let {
            it.setSession(mSession)
            it.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                onUpdateAvatarClick()
                false
            }
        }

        // Display name
        mDisplayNamePreference.let {
            it.summary = "TODO" // session.myUser.displayname
            it.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
                onDisplayNameClick(newValue?.let { (it as String).trim() })
                false
            }
        }

        // Password
        mPasswordPreference.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            onPasswordUpdateClick()
            false
        }

        // Add Email
        (findPreference(ADD_EMAIL_PREFERENCE_KEY) as EditTextPreference).let {
            // It does not work on XML, do it here
            it.icon = activity?.let {
                ThemeUtils.tintDrawable(it,
                        ContextCompat.getDrawable(it, R.drawable.ic_add_black)!!, R.attr.vctr_settings_icon_tint_color)
            }

            // Unfortunately, this is not supported in lib v7
            // it.editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS

            it.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
                addEmail((newValue as String).trim())
                false
            }
        }

        // Add phone number
        findPreference(ADD_PHONE_NUMBER_PREFERENCE_KEY).let {
            // It does not work on XML, do it here
            it.icon = activity?.let {
                ThemeUtils.tintDrawable(it,
                        ContextCompat.getDrawable(it, R.drawable.ic_add_black)!!, R.attr.vctr_settings_icon_tint_color)
            }

            it.setOnPreferenceClickListener {
                notImplemented()
                // TODO val intent = PhoneNumberAdditionActivity.getIntent(activity, session.credentials.userId)
                // startActivityForResult(intent, REQUEST_NEW_PHONE_NUMBER)
                true
            }
        }

        refreshEmailsList()
        refreshPhoneNumbersList()

        // Contacts
        setContactsPreferences()

        // user interface preferences
        setUserInterfacePreferences()

        // Url preview
        (findPreference(PreferencesManager.SETTINGS_SHOW_URL_PREVIEW_KEY) as SwitchPreference).let {
            /*
            TODO
            it.isChecked = session.isURLPreviewEnabled

            it.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
                if (null != newValue && newValue as Boolean != session.isURLPreviewEnabled) {
                    displayLoadingView()
                    session.setURLPreviewStatus(newValue, object : MatrixCallback<Unit> {
                        override fun onSuccess(info: Void?) {
                            it.isChecked = session.isURLPreviewEnabled
                            hideLoadingView()
                        }

                        private fun onError(errorMessage: String) {
                            activity?.toast(errorMessage)

                            onSuccess(null)
                        }

                        override fun onNetworkError(e: Exception) {
                            onError(e.localizedMessage)
                        }

                        override fun onMatrixError(e: MatrixError) {
                            onError(e.localizedMessage)
                        }

                        override fun onUnexpectedError(e: Exception) {
                            onError(e.localizedMessage)
                        }
                    })
                }

                false
            }
            */
        }

        // Themes
        findPreference(ThemeUtils.APPLICATION_THEME_KEY)
                .onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
            if (newValue is String) {
                vectorConfiguration.updateApplicationTheme(newValue)
                // Restart the Activity
                activity?.let {
                    // Note: recreate does not apply the color correctly
                    it.startActivity(it.intent)
                    it.finish()
                }
                true
            } else {
                false
            }
        }

        // Flair
        refreshGroupFlairsList()

        // push rules

        // Notification privacy
        mNotificationPrivacyPreference.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            notImplemented()
            // TODO startActivity(NotificationPrivacyActivity.getIntent(activity))
            true
        }
        refreshNotificationPrivacy()

        for (preferenceKey in mPrefKeyToBingRuleId.keys) {
            val preference = findPreference(preferenceKey)

            if (null != preference) {
                if (preference is SwitchPreference) {
                    preference.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValueAsVoid ->
                        // on some old android APIs,
                        // the callback is called even if there is no user interaction
                        // so the value will be checked to ensure there is really no update.
                        onPushRuleClick(preference.key, newValueAsVoid as Boolean)
                        true
                    }
                }
            }
        }

        // background sync tuning settings
        // these settings are useless and hidden if the app is registered to the FCM push service
        /*
        TODO
        val pushManager = Matrix.getInstance(appContext).pushManager
        if (pushManager.useFcm() && pushManager.hasRegistrationToken()) {
            // Hide the section
            preferenceScreen.removePreference(backgroundSyncDivider)
            preferenceScreen.removePreference(backgroundSyncCategory)
        } else {
            backgroundSyncPreference.let {
                it.isChecked = pushManager.isBackgroundSyncAllowed

                it.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, aNewValue ->
                    val newValue = aNewValue as Boolean

                    if (newValue != pushManager.isBackgroundSyncAllowed) {
                        pushManager.isBackgroundSyncAllowed = newValue
                    }

                    displayLoadingView()

                    Matrix.getInstance(activity)?.pushManager?.forceSessionsRegistration(object : MatrixCallback<Unit> {
                        override fun onSuccess(info: Void?) {
                            hideLoadingView()
                        }

                        override fun onMatrixError(e: MatrixError?) {
                            hideLoadingView()
                        }

                        override fun onNetworkError(e: java.lang.Exception?) {
                            hideLoadingView()
                        }

                        override fun onUnexpectedError(e: java.lang.Exception?) {
                            hideLoadingView()
                        }
                    })

                    true
                }
            }
        }
        */

        // Push target
        refreshPushersList()

        // Ignore users
        refreshIgnoredUsersList()

        // Lab
        val useCryptoPref = findPreference(PreferencesManager.SETTINGS_ROOM_SETTINGS_LABS_END_TO_END_PREFERENCE_KEY) as SwitchPreference
        val cryptoIsEnabledPref = findPreference(PreferencesManager.SETTINGS_ROOM_SETTINGS_LABS_END_TO_END_IS_ACTIVE_PREFERENCE_KEY)


        if (mSession.isCryptoEnabled()) {
            mLabsCategory.removePreference(useCryptoPref)

            cryptoIsEnabledPref.isEnabled = false
        } else {
            mLabsCategory.removePreference(cryptoIsEnabledPref)

            useCryptoPref.isChecked = false

            useCryptoPref.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValueAsVoid ->
                if (TextUtils.isEmpty(mSession.sessionParams.credentials.deviceId)) {
                    activity?.let { activity ->
                        AlertDialog.Builder(activity)
                                .setMessage(R.string.room_settings_labs_end_to_end_warnings)
                                .setPositiveButton(R.string.logout) { _, _ ->
                                    notImplemented()
                                    // TODO CommonActivityUtils.logout(activity)
                                }
                                .setNegativeButton(R.string.cancel) { _, _ ->
                                    useCryptoPref.isChecked = false
                                }
                                .setOnCancelListener {
                                    useCryptoPref.isChecked = false
                                }
                                .show()
                    }
                } else {
                    val newValue = newValueAsVoid as Boolean

                    if (mSession.isCryptoEnabled() != newValue) {
                        notImplemented()
                        /* TODO
                        displayLoadingView()

                        session.enableCrypto(newValue, object : MatrixCallback<Unit> {
                            private fun refresh() {
                                activity?.runOnUiThread {
                                    hideLoadingView()
                                    useCryptoPref.isChecked = session.isCryptoEnabled

                                    if (session.isCryptoEnabled) {
                                        mLabsCategory.removePreference(useCryptoPref)
                                        mLabsCategory.addPreference(cryptoIsEnabledPref)
                                    }
                                }
                            }

                            override fun onSuccess(info: Void?) {
                                useCryptoPref.isEnabled = false
                                refresh()
                            }

                            override fun onNetworkError(e: Exception) {
                                useCryptoPref.isChecked = false
                            }

                            override fun onMatrixError(e: MatrixError) {
                                useCryptoPref.isChecked = false
                            }

                            override fun onUnexpectedError(e: Exception) {
                                useCryptoPref.isChecked = false
                            }
                        })
                        */
                    }
                }

                true
            }
        }

        // SaveMode Management
        findPreference(PreferencesManager.SETTINGS_DATA_SAVE_MODE_PREFERENCE_KEY)
                .onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
            notImplemented()
            /* TODO
            val sessions = Matrix.getMXSessions(activity)
            for (session in sessions) {
                session.setUseDataSaveMode(newValue as Boolean)
            }
            */

            true
        }

        // Device list
        refreshDevicesList()

        //Refresh Key Management section
        refreshKeysManagementSection()

        // Advanced settings

        // user account
        findPreference(PreferencesManager.SETTINGS_LOGGED_IN_PREFERENCE_KEY)
                .summary = mSession.sessionParams.credentials.userId

        // home server
        findPreference(PreferencesManager.SETTINGS_HOME_SERVER_PREFERENCE_KEY)
                .summary = mSession.sessionParams.homeServerConnectionConfig.homeServerUri.toString()

        // identity server
        findPreference(PreferencesManager.SETTINGS_IDENTITY_SERVER_PREFERENCE_KEY)
                .summary = mSession.sessionParams.homeServerConnectionConfig.identityServerUri.toString()

        // Analytics

        // Analytics tracking management
        (findPreference(PreferencesManager.SETTINGS_USE_ANALYTICS_KEY) as SwitchPreference).let {
            // On if the analytics tracking is activated
            it.isChecked = PreferencesManager.useAnalytics(appContext)

            it.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
                PreferencesManager.setUseAnalytics(appContext, newValue as Boolean)
                true
            }
        }

        // Rageshake Management
        (findPreference(PreferencesManager.SETTINGS_USE_RAGE_SHAKE_KEY) as SwitchPreference).let {
            it.isChecked = PreferencesManager.useRageshake(appContext)

            it.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
                PreferencesManager.setUseRageshake(appContext, newValue as Boolean)
                true
            }
        }

        // preference to start the App info screen, to facilitate App permissions access
        findPreference(APP_INFO_LINK_PREFERENCE_KEY)
                .onPreferenceClickListener = Preference.OnPreferenceClickListener {

            activity?.let {
                val intent = Intent().apply {
                    action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

                    val uri = appContext?.let { Uri.fromParts("package", it.packageName, null) }

                    data = uri
                }
                it.applicationContext.startActivity(intent)
            }

            true
        }

        // application version
        (findPreference(PreferencesManager.SETTINGS_VERSION_PREFERENCE_KEY)).let {
            it.summary = getVersion(longFormat = false, useBuildNumber = true)

            it.setOnPreferenceClickListener { pref ->
                appContext?.let { ctx ->
                    copyToClipboard(ctx, pref.summary)
                }
                true
            }
        }

        // SDK version
        (findPreference(PreferencesManager.SETTINGS_SDK_VERSION_PREFERENCE_KEY)).let {
            it.summary = Matrix.getSdkVersion()

            it.setOnPreferenceClickListener { pref ->
                appContext?.let { ctx ->
                    copyToClipboard(ctx, pref.summary)
                }
                true
            }
        }

        // olm version
        findPreference(PreferencesManager.SETTINGS_OLM_VERSION_PREFERENCE_KEY)
                .summary = mSession.getCryptoVersion(requireContext(), false)

        // copyright
        findPreference(PreferencesManager.SETTINGS_COPYRIGHT_PREFERENCE_KEY)
                .onPreferenceClickListener = Preference.OnPreferenceClickListener {
            activity?.displayInWebView(VectorSettingsUrls.COPYRIGHT)
            false
        }

        // terms & conditions
        findPreference(PreferencesManager.SETTINGS_APP_TERM_CONDITIONS_PREFERENCE_KEY)
                .onPreferenceClickListener = Preference.OnPreferenceClickListener {
            activity?.displayInWebView(VectorSettingsUrls.TAC)
            false
        }

        // privacy policy
        findPreference(PreferencesManager.SETTINGS_PRIVACY_POLICY_PREFERENCE_KEY)
                .onPreferenceClickListener = Preference.OnPreferenceClickListener {
            activity?.displayInWebView(VectorSettingsUrls.PRIVACY_POLICY)
            false
        }

        // third party notice
        findPreference(PreferencesManager.SETTINGS_THIRD_PARTY_NOTICES_PREFERENCE_KEY)
                .onPreferenceClickListener = Preference.OnPreferenceClickListener {
            activity?.displayInWebView(VectorSettingsUrls.THIRD_PARTY_LICENSES)
            false
        }

        findPreference(PreferencesManager.SETTINGS_OTHER_THIRD_PARTY_NOTICES_PREFERENCE_KEY)
                .onPreferenceClickListener = Preference.OnPreferenceClickListener {
            // See https://developers.google.com/android/guides/opensource
            startActivity(Intent(requireActivity(), OssLicensesMenuActivity::class.java))
            false
        }

        // update keep medias period
        findPreference(PreferencesManager.SETTINGS_MEDIA_SAVING_PERIOD_KEY).let {
            it.summary = PreferencesManager.getSelectedMediasSavingPeriodString(activity)

            it.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                context?.let { context: Context ->
                    AlertDialog.Builder(context)
                            .setSingleChoiceItems(R.array.media_saving_choice,
                                    PreferencesManager.getSelectedMediasSavingPeriod(activity)) { d, n ->
                                PreferencesManager.setSelectedMediasSavingPeriod(activity, n)
                                d.cancel()

                                it.summary = PreferencesManager.getSelectedMediasSavingPeriodString(activity)
                            }
                            .show()
                }

                false
            }
        }

        // clear medias cache
        findPreference(PreferencesManager.SETTINGS_CLEAR_MEDIA_CACHE_PREFERENCE_KEY).let {
            /*
            TODO
            MXMediaCache.getCachesSize(activity, object : SimpleApiCallback<Long>() {
                override fun onSuccess(size: Long) {
                    if (null != activity) {
                        it.summary = android.text.format.Formatter.formatFileSize(activity, size)
                    }
                }
            })
            */

            it.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                notImplemented()
                /* TODO
                displayLoadingView()

                val task = ClearMediaCacheAsyncTask(
                        backgroundTask = {
                            session.mediaCache.clear()
                            activity?.let { it -> Glide.get(it).clearDiskCache() }
                        },
                        onCompleteTask = {
                            hideLoadingView()

                            MXMediaCache.getCachesSize(activity, object : SimpleApiCallback<Long>() {
                                override fun onSuccess(size: Long) {
                                    it.summary = Formatter.formatFileSize(activity, size)
                                }
                            })
                        }
                )

                try {
                    task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
                } catch (e: Exception) {
                    Timber.e(e, "## session.getMediaCache().clear() failed " + e.message)
                    task.cancel(true)
                    hideLoadingView()
                }
            */

                false
            }
        }

        // Incoming call sounds
        mUseRiotCallRingtonePreference.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            activity?.let { setUseRiotDefaultRingtone(it, mUseRiotCallRingtonePreference.isChecked) }
            false
        }

        mCallRingtonePreference.let {
            activity?.let { activity -> it.summary = getCallRingtoneName(activity) }
            it.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                displayRingtonePicker()
                false
            }
        }

        // clear cache
        findPreference(PreferencesManager.SETTINGS_CLEAR_CACHE_PREFERENCE_KEY).let {
            /*
            TODO
            MXSession.getApplicationSizeCaches(activity, object : SimpleApiCallback<Long>() {
                override fun onSuccess(size: Long) {
                    if (null != activity) {
                        it.summary = android.text.format.Formatter.formatFileSize(activity, size)
                    }
                }
            })
             */

            it.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                displayLoadingView()
                MainActivity.restartApp(activity!!, clearCache = true, clearCredentials = false)
                false
            }
        }

        // Deactivate account section

        // deactivate account
        findPreference(PreferencesManager.SETTINGS_DEACTIVATE_ACCOUNT_KEY)
                .onPreferenceClickListener = Preference.OnPreferenceClickListener {
            activity?.let {
                notImplemented()
                // TODO startActivity(DeactivateAccountActivity.getIntent(it))
            }

            false
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = super.onCreateView(inflater, container, savedInstanceState)

        view?.apply {
            val listView = findViewById<View>(android.R.id.list)
            listView?.setPadding(0, 0, 0, 0)
        }

        return view
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        // if the user toggles the contacts book permission
        /* TODO
        if (TextUtils.equals(key, ContactsManager.CONTACTS_BOOK_ACCESS_KEY)) {
            // reset the current snapshot
            ContactsManager.getInstance().clearSnapshot()
        }
        */
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is VectorSettingsFragmentInteractionListener) {
            interactionListener = context
        }
    }

    override fun onDetach() {
        interactionListener = null
        super.onDetach()
    }

    override fun onResume() {
        super.onResume()

        // find the view from parent activity
        mLoadingView = activity?.findViewById(R.id.vector_settings_spinner_views)

        /* TODO
        if (session.isAlive) {
            val context = activity?.applicationContext

            session.dataHandler.addListener(mEventsListener)

            Matrix.getInstance(context)?.addNetworkEventListener(mNetworkListener)

            session.myUser.refreshThirdPartyIdentifiers(object : SimpleApiCallback<Unit>() {
                override fun onSuccess(info: Void?) {
                    // ensure that the activity still exists
                    // and the result is called in the right thread
                    activity?.runOnUiThread {
                        refreshEmailsList()
                        refreshPhoneNumbersList()
                    }
                }
            })

            Matrix.getInstance(context)?.pushManager?.refreshPushersList(Matrix.getInstance(context)?.sessions, object : SimpleApiCallback<Unit>(activity) {
                override fun onSuccess(info: Void?) {
                    refreshPushersList()
                }
            })

            PreferenceManager.getDefaultSharedPreferences(context).registerOnSharedPreferenceChangeListener(this)

            // refresh anything else
            refreshPreferences()
            refreshNotificationPrivacy()
            refreshDisplay()
            refreshBackgroundSyncPrefs()
        }
        */

        interactionListener?.requestedKeyToHighlight()?.let { key ->
            interactionListener?.requestHighlightPreferenceKeyOnResume(null)
            val preference = findPreference(key)
            (preference as? VectorPreference)?.isHighlighted = true
        }
    }

    override fun onPause() {
        super.onPause()

        val context = activity?.applicationContext

        /* TODO
        if (session.isAlive) {
            session.dataHandler.removeListener(mEventsListener)
            Matrix.getInstance(context)?.removeNetworkEventListener(mNetworkListener)
        }
        */

        PreferenceManager.getDefaultSharedPreferences(context).unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (allGranted(grantResults)) {
            if (requestCode == PERMISSION_REQUEST_CODE_LAUNCH_CAMERA) {
                changeAvatar()
            } else if (requestCode == PERMISSION_REQUEST_CODE_EXPORT_KEYS) {
                exportKeys()
            }
        }
    }

    //==============================================================================================================
    // Display methods
    //==============================================================================================================

    /**
     * Display the loading view.
     */
    private fun displayLoadingView() {
        // search the loading view from the upper view
        if (null == mLoadingView) {
            var parent = view

            while (parent != null && mLoadingView == null) {
                mLoadingView = parent.findViewById(R.id.vector_settings_spinner_views)
                parent = parent.parent as View
            }
        } else {
            mLoadingView?.visibility = View.VISIBLE
        }
    }

    /**
     * Hide the loading view.
     */
    private fun hideLoadingView() {
        mLoadingView?.visibility = View.GONE
    }

    /**
     * Hide the loading view and refresh the preferences.
     *
     * @param refresh true to refresh the display
     */
    private fun hideLoadingView(refresh: Boolean) {
        mLoadingView?.visibility = View.GONE

        if (refresh) {
            refreshDisplay()
        }
    }

    /**
     * Refresh the preferences.
     */
    private fun refreshDisplay() {
        /* TODO
        // If Matrix instance is null, then connection can't be there
        val isConnected = Matrix.getInstance(activity)?.isConnected ?: false
        val appContext = activity?.applicationContext

        val preferenceManager = preferenceManager

        // refresh the avatar
        mUserAvatarPreference.refreshAvatar()
        mUserAvatarPreference.isEnabled = isConnected

        // refresh the display name
        mDisplayNamePreference.summary = session.myUser.displayname
        mDisplayNamePreference.text = session.myUser.displayname
        mDisplayNamePreference.isEnabled = isConnected

        // change password
        mPasswordPreference.isEnabled = isConnected

        // update the push rules
        val preferences = PreferenceManager.getDefaultSharedPreferences(appContext)

        val rules = session.dataHandler.pushRules()

        val pushManager = Matrix.getInstance(appContext)?.pushManager

        for (preferenceKey in mPrefKeyToBingRuleId.keys) {
            val preference = preferenceManager.findPreference(preferenceKey)

            if (null != preference) {

                if (preference is SwitchPreference) {
                    when (preferenceKey) {
                        PreferencesManager.SETTINGS_ENABLE_THIS_DEVICE_PREFERENCE_KEY ->
                            preference.isChecked = pushManager?.areDeviceNotificationsAllowed() ?: true

                        PreferencesManager.SETTINGS_TURN_SCREEN_ON_PREFERENCE_KEY -> {
                            preference.isChecked = pushManager?.isScreenTurnedOn ?: false
                            preference.isEnabled = pushManager?.areDeviceNotificationsAllowed() ?: true
                        }
                        else -> {
                            preference.isEnabled = null != rules && isConnected
                            preference.isChecked = preferences.getBoolean(preferenceKey, false)
                        }
                    }
                }
            }
        }

        // If notifications are disabled for the current user account or for the current user device
        // The others notifications settings have to be disable too
        val areNotificationAllowed = rules?.findDefaultRule(BingRule.RULE_ID_DISABLE_ALL)?.isEnabled == true

        mNotificationPrivacyPreference.isEnabled = !areNotificationAllowed
                && (pushManager?.areDeviceNotificationsAllowed() ?: true) && pushManager?.useFcm() ?: true
                */
    }

    //==============================================================================================================
    // Update items  methods
    //==============================================================================================================

    /**
     * Update the password.
     */
    private fun onPasswordUpdateClick() {
        activity?.let { activity ->
            val view: ViewGroup = activity.layoutInflater.inflate(R.layout.dialog_change_password, null) as ViewGroup

            val showPassword: ImageView = view.findViewById(R.id.change_password_show_passwords)
            val oldPasswordTil: TextInputLayout = view.findViewById(R.id.change_password_old_pwd_til)
            val oldPasswordText: TextInputEditText = view.findViewById(R.id.change_password_old_pwd_text)
            val newPasswordText: TextInputEditText = view.findViewById(R.id.change_password_new_pwd_text)
            val confirmNewPasswordTil: TextInputLayout = view.findViewById(R.id.change_password_confirm_new_pwd_til)
            val confirmNewPasswordText: TextInputEditText = view.findViewById(R.id.change_password_confirm_new_pwd_text)
            val changePasswordLoader: View = view.findViewById(R.id.change_password_loader)

            var passwordShown = false

            showPassword.setOnClickListener(object : View.OnClickListener {
                override fun onClick(v: View?) {
                    passwordShown = !passwordShown

                    oldPasswordText.showPassword(passwordShown)
                    newPasswordText.showPassword(passwordShown)
                    confirmNewPasswordText.showPassword(passwordShown)

                    showPassword.setImageResource(if (passwordShown) R.drawable.ic_eye_closed_black else R.drawable.ic_eye_black)
                }
            })

            val dialog = AlertDialog.Builder(activity)
                    .setView(view)
                    .setPositiveButton(R.string.settings_change_password_submit, null)
                    .setNegativeButton(R.string.cancel, null)
                    .setOnDismissListener {
                        val imm = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                        imm.hideSoftInputFromWindow(view.applicationWindowToken, 0)
                    }
                    .create()

            dialog.setOnShowListener {
                val updateButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                updateButton.isEnabled = false

                fun updateUi() {
                    val oldPwd = oldPasswordText.text.toString().trim()
                    val newPwd = newPasswordText.text.toString().trim()
                    val newConfirmPwd = confirmNewPasswordText.text.toString().trim()

                    updateButton.isEnabled = oldPwd.isNotEmpty() && newPwd.isNotEmpty() && TextUtils.equals(newPwd, newConfirmPwd)

                    if (newPwd.isNotEmpty() && newConfirmPwd.isNotEmpty() && !TextUtils.equals(newPwd, newConfirmPwd)) {
                        confirmNewPasswordTil.error = getString(R.string.passwords_do_not_match)
                    }
                }

                oldPasswordText.addTextChangedListener(object : SimpleTextWatcher() {
                    override fun afterTextChanged(s: Editable) {
                        oldPasswordTil.error = null
                        updateUi()
                    }
                })

                newPasswordText.addTextChangedListener(object : SimpleTextWatcher() {
                    override fun afterTextChanged(s: Editable) {
                        confirmNewPasswordTil.error = null
                        updateUi()
                    }
                })

                confirmNewPasswordText.addTextChangedListener(object : SimpleTextWatcher() {
                    override fun afterTextChanged(s: Editable) {
                        confirmNewPasswordTil.error = null
                        updateUi()
                    }
                })

                fun showPasswordLoadingView(toShow: Boolean) {
                    if (toShow) {
                        showPassword.isEnabled = false
                        oldPasswordText.isEnabled = false
                        newPasswordText.isEnabled = false
                        confirmNewPasswordText.isEnabled = false
                        changePasswordLoader.isVisible = true
                        updateButton.isEnabled = false
                    } else {
                        showPassword.isEnabled = true
                        oldPasswordText.isEnabled = true
                        newPasswordText.isEnabled = true
                        confirmNewPasswordText.isEnabled = true
                        changePasswordLoader.isVisible = false
                        updateButton.isEnabled = true
                    }
                }

                updateButton.setOnClickListener {
                    if (passwordShown) {
                        // Hide passwords during processing
                        showPassword.performClick()
                    }

                    val imm = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(view.applicationWindowToken, 0)

                    val oldPwd = oldPasswordText.text.toString().trim()
                    val newPwd = newPasswordText.text.toString().trim()

                    notImplemented()
                    /* TODO
                    showPasswordLoadingView(true)

                    session.updatePassword(oldPwd, newPwd, object : MatrixCallback<Unit> {
                        private fun onDone(@StringRes textResId: Int) {
                            showPasswordLoadingView(false)

                            if (textResId == R.string.settings_fail_to_update_password_invalid_current_password) {
                                oldPasswordTil.error = getString(textResId)
                            } else {
                                dialog.dismiss()
                                activity.toast(textResId, Toast.LENGTH_LONG)
                            }
                        }

                        override fun onSuccess(info: Void?) {
                            onDone(R.string.settings_password_updated)
                        }

                        override fun onNetworkError(e: Exception) {
                            onDone(R.string.settings_fail_to_update_password)
                        }

                        override fun onMatrixError(e: MatrixError) {
                            if (e.error == "Invalid password") {
                                onDone(R.string.settings_fail_to_update_password_invalid_current_password)
                            } else {
                                dialog.dismiss()
                                onDone(R.string.settings_fail_to_update_password)
                            }
                        }

                        override fun onUnexpectedError(e: Exception) {
                            onDone(R.string.settings_fail_to_update_password)
                        }
                    })
                    */
                }
            }
            dialog.show()
        }
    }

    /**
     * Update a push rule.
     */

    private fun onPushRuleClick(preferenceKey: String, newValue: Boolean) {
        notImplemented()
        /* TODO
        val matrixInstance = Matrix.getInstance(context)
        val pushManager = matrixInstance.pushManager

        Timber.v("onPushRuleClick $preferenceKey : set to $newValue")

        when (preferenceKey) {

            PreferencesManager.SETTINGS_TURN_SCREEN_ON_PREFERENCE_KEY -> {
                if (pushManager.isScreenTurnedOn != newValue) {
                    pushManager.isScreenTurnedOn = newValue
                }
            }

            PreferencesManager.SETTINGS_ENABLE_THIS_DEVICE_PREFERENCE_KEY -> {
                val isConnected = matrixInstance.isConnected
                val isAllowed = pushManager.areDeviceNotificationsAllowed()

                // avoid useless update
                if (isAllowed == newValue) {
                    return
                }

                pushManager.setDeviceNotificationsAllowed(!isAllowed)

                // when using FCM
                // need to register on servers
                if (isConnected && pushManager.useFcm() && (pushManager.isServerRegistered || pushManager.isServerUnRegistered)) {
                    val listener = object : MatrixCallback<Unit> {

                        private fun onDone() {
                            activity?.runOnUiThread {
                                hideLoadingView(true)
                                refreshPushersList()
                            }
                        }

                        override fun onSuccess(info: Void?) {
                            onDone()
                        }

                        override fun onMatrixError(e: MatrixError?) {
                            // Set again the previous state
                            pushManager.setDeviceNotificationsAllowed(isAllowed)
                            onDone()
                        }

                        override fun onNetworkError(e: java.lang.Exception?) {
                            // Set again the previous state
                            pushManager.setDeviceNotificationsAllowed(isAllowed)
                            onDone()
                        }

                        override fun onUnexpectedError(e: java.lang.Exception?) {
                            // Set again the previous state
                            pushManager.setDeviceNotificationsAllowed(isAllowed)
                            onDone()
                        }
                    }

                    displayLoadingView()
                    if (pushManager.isServerRegistered) {
                        pushManager.unregister(listener)
                    } else {
                        pushManager.register(listener)
                    }
                }
            }

            // check if there is an update

            // on some old android APIs,
            // the callback is called even if there is no user interaction
            // so the value will be checked to ensure there is really no update.
            else -> {

                val ruleId = mPrefKeyToBingRuleId[preferenceKey]
                val rule = session.dataHandler.pushRules()?.findDefaultRule(ruleId)

                // check if there is an update
                var curValue = null != rule && rule.isEnabled

                if (TextUtils.equals(ruleId, BingRule.RULE_ID_DISABLE_ALL) || TextUtils.equals(ruleId, BingRule.RULE_ID_SUPPRESS_BOTS_NOTIFICATIONS)) {
                    curValue = !curValue
                }

                // on some old android APIs,
                // the callback is called even if there is no user interaction
                // so the value will be checked to ensure there is really no update.
                if (newValue == curValue) {
                    return
                }

                if (null != rule) {
                    displayLoadingView()
                    session.dataHandler.bingRulesManager.updateEnableRuleStatus(rule, !rule.isEnabled, object : BingRulesManager.onBingRuleUpdateListener {
                        private fun onDone() {
                            refreshDisplay()
                            hideLoadingView()
                        }

                        override fun onBingRuleUpdateSuccess() {
                            onDone()
                        }

                        override fun onBingRuleUpdateFailure(errorMessage: String) {
                            activity?.toast(errorMessage)
                            onDone()
                        }
                    })
                }
            }
        }
        */
    }

    /**
     * Update the displayname.
     */
    private fun onDisplayNameClick(value: String?) {
        notImplemented()
        /* TODO
        if (!TextUtils.equals(session.myUser.displayname, value)) {
            displayLoadingView()

            session.myUser.updateDisplayName(value, object : MatrixCallback<Unit> {
                override fun onSuccess(info: Void?) {
                    // refresh the settings value
                    PreferenceManager.getDefaultSharedPreferences(activity).edit {
                        putString(PreferencesManager.SETTINGS_DISPLAY_NAME_PREFERENCE_KEY, value)
                    }

                    onCommonDone(null)

                    refreshDisplay()
                }

                override fun onNetworkError(e: Exception) {
                    onCommonDone(e.localizedMessage)
                }

                override fun onMatrixError(e: MatrixError) {
                    if (MatrixError.M_CONSENT_NOT_GIVEN == e.errcode) {
                        activity?.runOnUiThread {
                            hideLoadingView()
                            (activity as VectorAppCompatActivity).consentNotGivenHelper.displayDialog(e)
                        }
                    } else {
                        onCommonDone(e.localizedMessage)
                    }
                }

                override fun onUnexpectedError(e: Exception) {
                    onCommonDone(e.localizedMessage)
                }
            })
        }
        */
    }

    private fun displayRingtonePicker() {
        val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
            putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, getString(R.string.settings_call_ringtone_dialog_title))
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
            putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_RINGTONE)
            activity?.let { putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, getCallRingtoneUri(it)) }
        }
        startActivityForResult(intent, REQUEST_CALL_RINGTONE)
    }

    /**
     * Update the avatar.
     */
    private fun onUpdateAvatarClick() {
        notImplemented()

        /* TODO
        if (checkPermissions(PERMISSIONS_FOR_TAKING_PHOTO, this, PERMISSION_REQUEST_CODE_LAUNCH_CAMERA)) {
            changeAvatar()
        }
        */
    }

    private fun changeAvatar() {
        /* TODO
        val intent = Intent(activity, VectorMediaPickerActivity::class.java)
        intent.putExtra(VectorMediaPickerActivity.EXTRA_AVATAR_MODE, true)
        startActivityForResult(intent, VectorUtils.TAKE_IMAGE)
        */
    }

    /**
     * Refresh the notification privacy setting
     */
    private fun refreshNotificationPrivacy() {
        /* TODO
        val pushManager = Matrix.getInstance(activity).pushManager

        // this setting apply only with FCM for the moment
        if (pushManager.useFcm()) {
            val notificationPrivacyString = NotificationPrivacyActivity.getNotificationPrivacyString(activity,
                    pushManager.notificationPrivacy)
            mNotificationPrivacyPreference.summary = notificationPrivacyString
        } else {
            notificationsSettingsCategory.removePreference(mNotificationPrivacyPreference)
        }
        */
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                REQUEST_CALL_RINGTONE         -> {
                    val callRingtoneUri: Uri? = data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
                    val thisActivity = activity
                    if (callRingtoneUri != null && thisActivity != null) {
                        setCallRingtoneUri(thisActivity, callRingtoneUri)
                        mCallRingtonePreference.summary = getCallRingtoneName(thisActivity)
                    }
                }
                REQUEST_E2E_FILE_REQUEST_CODE -> importKeys(data)
                REQUEST_NEW_PHONE_NUMBER      -> refreshPhoneNumbersList()
                REQUEST_PHONEBOOK_COUNTRY     -> onPhonebookCountryUpdate(data)
                REQUEST_LOCALE                -> {
                    activity?.let {
                        startActivity(it.intent)
                        it.finish()
                    }
                }
                /* TODO
                VectorUtils.TAKE_IMAGE -> {
                    val thumbnailUri = VectorUtils.getThumbnailUriFromIntent(activity, data, session.mediaCache)

                    if (null != thumbnailUri) {
                        displayLoadingView()

                        val resource = ResourceUtils.openResource(activity, thumbnailUri, null)

                        if (null != resource) {
                            session.mediaCache.uploadContent(resource.mContentStream, null, resource.mMimeType, null, object : MXMediaUploadListener() {

                                override fun onUploadError(uploadId: String?, serverResponseCode: Int, serverErrorMessage: String?) {
                                    activity?.runOnUiThread { onCommonDone(serverResponseCode.toString() + " : " + serverErrorMessage) }
                                }

                                override fun onUploadComplete(uploadId: String?, contentUri: String?) {
                                    activity?.runOnUiThread {
                                        session.myUser.updateAvatarUrl(contentUri, object : MatrixCallback<Unit> {
                                            override fun onSuccess(info: Void?) {
                                                onCommonDone(null)
                                                refreshDisplay()
                                            }

                                            override fun onNetworkError(e: Exception) {
                                                onCommonDone(e.localizedMessage)
                                            }

                                            override fun onMatrixError(e: MatrixError) {
                                                if (MatrixError.M_CONSENT_NOT_GIVEN == e.errcode) {
                                                    activity?.runOnUiThread {
                                                        hideLoadingView()
                                                        (activity as VectorAppCompatActivity).consentNotGivenHelper.displayDialog(e)
                                                    }
                                                } else {
                                                    onCommonDone(e.localizedMessage)
                                                }
                                            }

                                            override fun onUnexpectedError(e: Exception) {
                                                onCommonDone(e.localizedMessage)
                                            }
                                        })
                                    }
                                }
                            })
                        }
                    }
                }
                */
            }
        }
    }

    /**
     * Refresh the known information about the account
     */
    private fun refreshPreferences() {
        PreferenceManager.getDefaultSharedPreferences(activity).edit {
            putString(PreferencesManager.SETTINGS_DISPLAY_NAME_PREFERENCE_KEY, "TODO") //session.myUser.displayname)

            /* TODO
            session.dataHandler.pushRules()?.let {
                for (preferenceKey in mPrefKeyToBingRuleId.keys) {
                    val preference = findPreference(preferenceKey)

                    if (null != preference && preference is SwitchPreference) {
                        val ruleId = mPrefKeyToBingRuleId[preferenceKey]

                        val rule = it.findDefaultRule(ruleId)
                        var isEnabled = null != rule && rule.isEnabled

                        if (TextUtils.equals(ruleId, BingRule.RULE_ID_DISABLE_ALL) || TextUtils.equals(ruleId, BingRule.RULE_ID_SUPPRESS_BOTS_NOTIFICATIONS)) {
                            isEnabled = !isEnabled
                        } else if (isEnabled) {
                            val domainActions = rule?.domainActions

                            // no action -> noting will be done
                            if (null == domainActions || domainActions.isEmpty()) {
                                isEnabled = false
                            } else if (1 == domainActions.size) {
                                try {
                                    isEnabled = !TextUtils.equals(domainActions[0] as String, BingRule.ACTION_DONT_NOTIFY)
                                } catch (e: Exception) {
                                    Timber.e(e, "## refreshPreferences failed " + e.message)
                                }

                            }
                        }// check if the rule is only defined by don't notify

                        putBoolean(preferenceKey, isEnabled)
                    }
                }
            }
            */
        }
    }

    /**
     * Display a dialog which asks confirmation for the deletion of a 3pid
     *
     * @param pid               the 3pid to delete
     * @param preferenceSummary the displayed 3pid
     */
    private fun displayDelete3PIDConfirmationDialog(/* TODO pid: ThirdPartyIdentifier,*/ preferenceSummary: CharSequence) {
        val mediumFriendlyName = "TODO" // ThreePid.getMediumFriendlyName(pid.medium, activity).toLowerCase(VectorLocale.applicationLocale)
        val dialogMessage = getString(R.string.settings_delete_threepid_confirmation, mediumFriendlyName, preferenceSummary)

        activity?.let {
            AlertDialog.Builder(it)
                    .setTitle(R.string.dialog_title_confirmation)
                    .setMessage(dialogMessage)
                    .setPositiveButton(R.string.remove) { _, _ ->
                        notImplemented()
                        /* TODO
                        displayLoadingView()

                        session.myUser.delete3Pid(pid, object : MatrixCallback<Unit> {
                            override fun onSuccess(info: Void?) {
                                when (pid.medium) {
                                    ThreePid.MEDIUM_EMAIL -> refreshEmailsList()
                                    ThreePid.MEDIUM_MSISDN -> refreshPhoneNumbersList()
                                }
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
                        */
                    }
                    .setNegativeButton(R.string.cancel, null)
                    .show()
        }
    }

    //==============================================================================================================
    // ignored users list management
    //==============================================================================================================

    /**
     * Refresh the ignored users list
     */
    private fun refreshIgnoredUsersList() {
        val ignoredUsersList = mutableListOf<String>() // TODO session.dataHandler.ignoredUserIds

        ignoredUsersList.sortWith(Comparator { u1, u2 ->
            u1.toLowerCase(VectorLocale.applicationLocale).compareTo(u2.toLowerCase(VectorLocale.applicationLocale))
        })

        val preferenceScreen = preferenceScreen

        preferenceScreen.removePreference(mIgnoredUserSettingsCategory)
        preferenceScreen.removePreference(mIgnoredUserSettingsCategoryDivider)
        mIgnoredUserSettingsCategory.removeAll()

        if (ignoredUsersList.size > 0) {
            preferenceScreen.addPreference(mIgnoredUserSettingsCategoryDivider)
            preferenceScreen.addPreference(mIgnoredUserSettingsCategory)

            for (userId in ignoredUsersList) {
                val preference = Preference(activity)

                preference.title = userId
                preference.key = IGNORED_USER_KEY_BASE + userId

                preference.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                    activity?.let {
                        AlertDialog.Builder(it)
                                .setMessage(getString(R.string.settings_unignore_user, userId))
                                .setPositiveButton(R.string.yes) { _, _ ->
                                    displayLoadingView()

                                    val idsList = ArrayList<String>()
                                    idsList.add(userId)

                                    notImplemented()
                                    /* TODO
                                    session.unIgnoreUsers(idsList, object : MatrixCallback<Unit> {
                                        override fun onSuccess(info: Void?) {
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
                                    */
                                }
                                .setNegativeButton(R.string.no, null)
                                .show()
                    }

                    false
                }

                mIgnoredUserSettingsCategory.addPreference(preference)
            }
        }
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

    //==============================================================================================================
    // Email management
    //==============================================================================================================

    /**
     * Refresh the emails list
     */
    private fun refreshEmailsList() {
        val currentEmail3PID = emptyList<String>() // TODO ArrayList(session.myUser.getlinkedEmails())

        val newEmailsList = ArrayList<String>()
        for (identifier in currentEmail3PID) {
            // TODO newEmailsList.add(identifier.address)
        }

        // check first if there is an update
        var isNewList = true
        if (newEmailsList.size == mDisplayedEmails.size) {
            isNewList = !mDisplayedEmails.containsAll(newEmailsList)
        }

        if (isNewList) {
            // remove the displayed one
            run {
                var index = 0
                while (true) {
                    val preference = mUserSettingsCategory.findPreference(EMAIL_PREFERENCE_KEY_BASE + index)

                    if (null != preference) {
                        mUserSettingsCategory.removePreference(preference)
                    } else {
                        break
                    }
                    index++
                }
            }

            // add new emails list
            mDisplayedEmails = newEmailsList

            val addEmailBtn = mUserSettingsCategory.findPreference(ADD_EMAIL_PREFERENCE_KEY)
                    ?: return

            var order = addEmailBtn.order

            for ((index, email3PID) in currentEmail3PID.withIndex()) {
                val preference = VectorPreference(activity!!)

                preference.title = getString(R.string.settings_email_address)
                preference.summary = "TODO" // email3PID.address
                preference.key = EMAIL_PREFERENCE_KEY_BASE + index
                preference.order = order

                preference.onPreferenceClickListener = Preference.OnPreferenceClickListener { pref ->
                    displayDelete3PIDConfirmationDialog(/* TODO email3PID, */ pref.summary)
                    true
                }

                preference.onPreferenceLongClickListener = object : VectorPreference.OnPreferenceLongClickListener {
                    override fun onPreferenceLongClick(preference: Preference): Boolean {
                        activity?.let { copyToClipboard(it, "TODO") } //email3PID.address) }
                        return true
                    }
                }

                mUserSettingsCategory.addPreference(preference)

                order++
            }

            addEmailBtn.order = order
        }
    }

    /**
     * A request has been processed.
     * Display a toast if there is a an error message
     *
     * @param errorMessage the error message
     */
    private fun onCommonDone(errorMessage: String?) {
        if (!isAdded) {
            return
        }
        activity?.runOnUiThread {
            if (!TextUtils.isEmpty(errorMessage) && errorMessage != null) {
                activity?.toast(errorMessage!!)
            }
            hideLoadingView()
        }
    }

    /**
     * Attempt to add a new email to the account
     *
     * @param email the email to add.
     */
    private fun addEmail(email: String) {
        // check first if the email syntax is valid
        // if email is null , then also its invalid email
        if (TextUtils.isEmpty(email) || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            activity?.toast(R.string.auth_invalid_email)
            return
        }

        // check first if the email syntax is valid
        if (mDisplayedEmails.indexOf(email) >= 0) {
            activity?.toast(R.string.auth_email_already_defined)
            return
        }

        notImplemented()
        /* TODO
        val pid = ThreePid(email, ThreePid.MEDIUM_EMAIL)

        displayLoadingView()

        session.myUser.requestEmailValidationToken(pid, object : MatrixCallback<Unit> {
            override fun onSuccess(info: Void?) {
                activity?.runOnUiThread { showEmailValidationDialog(pid) }
            }

            override fun onNetworkError(e: Exception) {
                onCommonDone(e.localizedMessage)
            }

            override fun onMatrixError(e: MatrixError) {
                if (TextUtils.equals(MatrixError.THREEPID_IN_USE, e.errcode)) {
                    onCommonDone(getString(R.string.account_email_already_used_error))
                } else {
                    onCommonDone(e.localizedMessage)
                }
            }

            override fun onUnexpectedError(e: Exception) {
                onCommonDone(e.localizedMessage)
            }
        })
        */
    }

    /**
     * Show an email validation dialog to warn the user tho valid his email link.
     *
     * @param pid the used pid.
     */
    /* TODO
    private fun showEmailValidationDialog(pid: ThreePid) {
        activity?.let {
            AlertDialog.Builder(it)
                    .setTitle(R.string.account_email_validation_title)
                    .setMessage(R.string.account_email_validation_message)
                    .setPositiveButton(R.string._continue) { _, _ ->
                        session.myUser.add3Pid(pid, true, object : MatrixCallback<Unit> {
                            override fun onSuccess(info: Void?) {
                                it.runOnUiThread {
                                    hideLoadingView()
                                    refreshEmailsList()
                                }
                            }

                            override fun onNetworkError(e: Exception) {
                                onCommonDone(e.localizedMessage)
                            }

                            override fun onMatrixError(e: MatrixError) {
                                if (TextUtils.equals(e.errcode, MatrixError.THREEPID_AUTH_FAILED)) {
                                    it.runOnUiThread {
                                        hideLoadingView()
                                        it.toast(R.string.account_email_validation_error)
                                    }
                                } else {
                                    onCommonDone(e.localizedMessage)
                                }
                            }

                            override fun onUnexpectedError(e: Exception) {
                                onCommonDone(e.localizedMessage)
                            }
                        })
                    }
                    .setNegativeButton(R.string.cancel) { _, _ ->
                        hideLoadingView()
                    }
                    .show()
        }
    }    */

    //==============================================================================================================
    // Phone number management
    //==============================================================================================================

    /**
     * Refresh phone number list
     */
    private fun refreshPhoneNumbersList() {
        /* TODO
        val currentPhoneNumber3PID = ArrayList(session.myUser.getlinkedPhoneNumbers())

        val phoneNumberList = ArrayList<String>()
        for (identifier in currentPhoneNumber3PID) {
            phoneNumberList.add(identifier.address)
        }

        // check first if there is an update
        var isNewList = true
        if (phoneNumberList.size == mDisplayedPhoneNumber.size) {
            isNewList = !mDisplayedPhoneNumber.containsAll(phoneNumberList)
        }

        if (isNewList) {
            // remove the displayed one
            run {
                var index = 0
                while (true) {
                    val preference = mUserSettingsCategory.findPreference(PHONE_NUMBER_PREFERENCE_KEY_BASE + index)

                    if (null != preference) {
                        mUserSettingsCategory.removePreference(preference)
                    } else {
                        break
                    }
                    index++
                }
            }

            // add new phone number list
            mDisplayedPhoneNumber = phoneNumberList

            val addPhoneBtn = mUserSettingsCategory.findPreference(ADD_PHONE_NUMBER_PREFERENCE_KEY)
                    ?: return

            var order = addPhoneBtn.order

            for ((index, phoneNumber3PID) in currentPhoneNumber3PID.withIndex()) {
                val preference = VectorPreference(activity!!)

                preference.title = getString(R.string.settings_phone_number)
                var phoneNumberFormatted = phoneNumber3PID.address
                try {
                    // Attempt to format phone number
                    val phoneNumber = PhoneNumberUtil.getInstance().parse("+$phoneNumberFormatted", null)
                    phoneNumberFormatted = PhoneNumberUtil.getInstance().format(phoneNumber, PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL)
                } catch (e: NumberParseException) {
                    // Do nothing, we will display raw version
                }

                preference.summary = phoneNumberFormatted
                preference.key = PHONE_NUMBER_PREFERENCE_KEY_BASE + index
                preference.order = order

                preference.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                    displayDelete3PIDConfirmationDialog(phoneNumber3PID, preference.summary)
                    true
                }

                preference.onPreferenceLongClickListener = object : VectorPreference.OnPreferenceLongClickListener {
                    override fun onPreferenceLongClick(preference: Preference): Boolean {
                        activity?.let { copyToClipboard(it, phoneNumber3PID.address) }
                        return true
                    }
                }

                order++
                mUserSettingsCategory.addPreference(preference)
            }

            addPhoneBtn.order = order
        }    */
    }

    //==============================================================================================================
    // contacts management
    //==============================================================================================================

    private fun setContactsPreferences() {
        /* TODO
        // Permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // on Android >= 23, use the system one
            mContactSettingsCategory.removePreference(findPreference(ContactsManager.CONTACTS_BOOK_ACCESS_KEY))
        }
        // Phonebook country
        mContactPhonebookCountryPreference.summary = PhoneNumberUtils.getHumanCountryCode(PhoneNumberUtils.getCountryCode(activity))

        mContactPhonebookCountryPreference.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            val intent = CountryPickerActivity.getIntent(activity, true)
            startActivityForResult(intent, REQUEST_PHONEBOOK_COUNTRY)
            true
        }
        */
    }

    private fun onPhonebookCountryUpdate(data: Intent?) {
        /* TODO
        if (data != null && data.hasExtra(CountryPickerActivity.EXTRA_OUT_COUNTRY_NAME)
                && data.hasExtra(CountryPickerActivity.EXTRA_OUT_COUNTRY_CODE)) {
            val countryCode = data.getStringExtra(CountryPickerActivity.EXTRA_OUT_COUNTRY_CODE)
            if (!TextUtils.equals(countryCode, PhoneNumberUtils.getCountryCode(activity))) {
                PhoneNumberUtils.setCountryCode(activity, countryCode)
                mContactPhonebookCountryPreference.summary = data.getStringExtra(CountryPickerActivity.EXTRA_OUT_COUNTRY_NAME)
            }
        }
        */
    }

    //==============================================================================================================
    // user interface management
    //==============================================================================================================

    private fun setUserInterfacePreferences() {
        // Selected language
        selectedLanguagePreference.summary = VectorLocale.localeToLocalisedString(VectorLocale.applicationLocale)

        selectedLanguagePreference.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            notImplemented()
            // TODO startActivityForResult(LanguagePickerActivity.getIntent(activity), REQUEST_LOCALE)
            true
        }

        // Text size
        textSizePreference.summary = FontScale.getFontScaleDescription(activity!!)

        textSizePreference.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            activity?.let { displayTextSizeSelection(it) }
            true
        }
    }

    private fun displayTextSizeSelection(activity: Activity) {
        val inflater = activity.layoutInflater
        val layout = inflater.inflate(R.layout.dialog_select_text_size, null)

        val dialog = AlertDialog.Builder(activity)
                .setTitle(R.string.font_size)
                .setView(layout)
                .setPositiveButton(R.string.ok, null)
                .setNegativeButton(R.string.cancel, null)
                .show()

        val linearLayout = layout.findViewById<LinearLayout>(R.id.text_selection_group_view)

        val childCount = linearLayout.childCount

        val scaleText = FontScale.getFontScaleDescription(activity)

        for (i in 0 until childCount) {
            val v = linearLayout.getChildAt(i)

            if (v is CheckedTextView) {
                v.isChecked = TextUtils.equals(v.text, scaleText)

                v.setOnClickListener {
                    dialog.dismiss()
                    FontScale.updateFontScale(activity, v.text.toString())
                    activity.startActivity(activity.intent)
                    activity.finish()
                }
            }
        }
    }

    //==============================================================================================================
    // background sync management
    //==============================================================================================================

    /**
     * Convert a delay in seconds to string
     *
     * @param seconds the delay in seconds
     * @return the text
     */
    private fun secondsToText(seconds: Int): String {
        return if (seconds > 1) {
            seconds.toString() + " " + getString(R.string.settings_seconds)
        } else {
            seconds.toString() + " " + getString(R.string.settings_second)
        }
    }

    /**
     * Refresh the background sync preference
     */
    private fun refreshBackgroundSyncPrefs() {
        /* TODO
        activity?.let { activity ->
            val pushManager = Matrix.getInstance(activity).pushManager

            val timeout = pushManager.backgroundSyncTimeOut / 1000
            val delay = pushManager.backgroundSyncDelay / 1000

            // update the settings
            PreferenceManager.getDefaultSharedPreferences(activity).edit {
                putString(PreferencesManager.SETTINGS_SET_SYNC_TIMEOUT_PREFERENCE_KEY, timeout.toString() + "")
                putString(PreferencesManager.SETTINGS_SET_SYNC_DELAY_PREFERENCE_KEY, delay.toString() + "")
            }

            mSyncRequestTimeoutPreference?.let {
                it.summary = secondsToText(timeout)
                it.text = timeout.toString() + ""

                it.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
                    var newTimeOut = timeout

                    try {
                        newTimeOut = Integer.parseInt(newValue as String)
                    } catch (e: Exception) {
                        Timber.e(e, "## refreshBackgroundSyncPrefs : parseInt failed " + e.message)
                    }

                    if (newTimeOut != timeout) {
                        pushManager.backgroundSyncTimeOut = newTimeOut * 1000

                        activity.runOnUiThread { refreshBackgroundSyncPrefs() }
                    }

                    false
                }
            }

            mSyncRequestDelayPreference?.let {
                it.summary = secondsToText(delay)
                it.text = delay.toString() + ""

                it.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
                    var newDelay = delay

                    try {
                        newDelay = Integer.parseInt(newValue as String)
                    } catch (e: Exception) {
                        Timber.e(e, "## refreshBackgroundSyncPrefs : parseInt failed " + e.message)
                    }

                    if (newDelay != delay) {
                        pushManager.backgroundSyncDelay = newDelay * 1000

                        activity.runOnUiThread { refreshBackgroundSyncPrefs() }
                    }

                    false
                }
            }
        }
        */
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
        val userId = mSession.sessionParams.credentials.userId
        val deviceId = mSession.sessionParams.credentials.deviceId

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
            val deviceInfo = mSession.getDeviceInfo(userId, deviceId)

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

        sendToUnverifiedDevicesPref.isChecked = mSession.getGlobalBlacklistUnverifiedDevices()

        sendToUnverifiedDevicesPref.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            mSession.setGlobalBlacklistUnverifiedDevices(sendToUnverifiedDevicesPref.isChecked)

            true
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
        if (mSession.isCryptoEnabled() && !TextUtils.isEmpty(mSession.sessionParams.credentials.deviceId)) {
            // display a spinner while loading the devices list
            if (0 == mDevicesListSettingsCategory.preferenceCount) {
                activity?.let {
                    val preference = ProgressBarPreference(it)
                    mDevicesListSettingsCategory.addPreference(preference)
                }
            }

            mSession.getDevicesList(object : MatrixCallback<DevicesListResponse> {
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
        val myDeviceId = mSession.sessionParams.credentials.deviceId

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
            if (!TextUtils.equals(mSession.getMyDevice()?.deviceId, aDeviceInfo.deviceId)) {
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

                        mSession.setDeviceName(aDeviceInfoToRename.deviceId!!, newName, object : MatrixCallback<Unit> {
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
        mSession.deleteDevice(deviceId, object : MatrixCallback<Unit> {
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
        mSession.deleteDeviceWithUserPassword(deviceId, authSession, accountPassword, object : MatrixCallback<Unit> {
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

                        KeysExporter(mSession)
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

                KeysImporter(mSession)
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
    // Group flairs management
    //==============================================================================================================

    /**
     * Force the refresh of the devices list.<br></br>
     * The devices list is the list of the devices where the user as looged in.
     * It can be any mobile device, as any browser.
     */
    private fun refreshGroupFlairsList() {
        // display a spinner while refreshing
        if (0 == mGroupsFlairCategory.preferenceCount) {
            activity?.let {
                val preference = ProgressBarPreference(it)
                mGroupsFlairCategory.addPreference(preference)
            }
        }

        /*
        TODO
        session.groupsManager.getUserPublicisedGroups(session.myUserId, true, object : MatrixCallback<Set<String>> {
            override fun onSuccess(publicisedGroups: Set<String>) {
                // clear everything
                mGroupsFlairCategory.removeAll()

                if (publicisedGroups.isEmpty()) {
                    val vectorGroupPreference = Preference(activity)
                    vectorGroupPreference.title = resources.getString(R.string.settings_without_flair)
                    mGroupsFlairCategory.addPreference(vectorGroupPreference)
                } else {
                    buildGroupsList(publicisedGroups)
                }
            }

            override fun onNetworkError(e: Exception) {
                // NOP
            }

            override fun onMatrixError(e: MatrixError) {
                // NOP
            }

            override fun onUnexpectedError(e: Exception) {
                // NOP
            }
        })
        */
    }

    /**
     * Build the groups list.
     *
     * @param publicisedGroups the publicised groups list.
     */
    private fun buildGroupsList(publicisedGroups: Set<String>) {
        var isNewList = true

        mPublicisedGroups?.let {
            if (it.size == publicisedGroups.size) {
                isNewList = !it.containsAll(publicisedGroups)
            }
        }

        if (isNewList) {
            /*
            TODO
            val joinedGroups = ArrayList(session.groupsManager.joinedGroups)
            Collections.sort(joinedGroups, Group.mGroupsComparator)

            mPublicisedGroups = publicisedGroups.toMutableSet()

            for ((prefIndex, group) in joinedGroups.withIndex()) {
                val vectorGroupPreference = VectorGroupPreference(activity!!)
                vectorGroupPreference.key = DEVICES_PREFERENCE_KEY_BASE + prefIndex

                vectorGroupPreference.setGroup(group, session)
                vectorGroupPreference.title = group.displayName
                vectorGroupPreference.summary = group.groupId

                vectorGroupPreference.isChecked = publicisedGroups.contains(group.groupId)
                mGroupsFlairCategory.addPreference(vectorGroupPreference)

                vectorGroupPreference.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
                    if (newValue is Boolean) {
                        /*
                         *  if mPublicisedGroup is null somehow, then
                         *  we cant check it contains groupId or not
                         *  so set isFlaired to false
                        */
                        val isFlaired = mPublicisedGroups?.contains(group.groupId) ?: false

                        if (newValue != isFlaired) {
                            displayLoadingView()
                            session.groupsManager.updateGroupPublicity(group.groupId, newValue, object : MatrixCallback<Unit> {
                                override fun onSuccess(info: Void?) {
                                    hideLoadingView()
                                    if (newValue) {
                                        mPublicisedGroups?.add(group.groupId)
                                    } else {
                                        mPublicisedGroups?.remove(group.groupId)
                                    }
                                }

                                private fun onError() {
                                    hideLoadingView()
                                    // restore default value
                                    vectorGroupPreference.isChecked = publicisedGroups.contains(group.groupId)
                                }

                                override fun onNetworkError(e: Exception) {
                                    onError()
                                }

                                override fun onMatrixError(e: MatrixError) {
                                    onError()
                                }

                                override fun onUnexpectedError(e: Exception) {
                                    onError()
                                }
                            })
                        }
                    }
                    true
                }
            }
                */

            refreshCryptographyPreference(mMyDeviceInfo)
        }
    }

    private class ClearMediaCacheAsyncTask internal constructor(
            backgroundTask: () -> Unit,
            onCompleteTask: () -> Unit
    ) : AsyncTask<Unit, Unit, Unit>() {

        private val backgroundTaskReference = WeakReference(backgroundTask)
        private val onCompleteTaskReference = WeakReference(onCompleteTask)
        override fun doInBackground(vararg params: Unit?) {
            backgroundTaskReference.get()?.invoke()
        }

        override fun onPostExecute(result: Unit?) {
            super.onPostExecute(result)
            onCompleteTaskReference.get()?.invoke()
        }
    }

    /* ==========================================================================================
     * Companion
     * ========================================================================================== */

    companion object {
        // arguments indexes
        private const val ARG_MATRIX_ID = "VectorSettingsPreferencesFragment.ARG_MATRIX_ID"

        private const val EMAIL_PREFERENCE_KEY_BASE = "EMAIL_PREFERENCE_KEY_BASE"
        private const val PHONE_NUMBER_PREFERENCE_KEY_BASE = "PHONE_NUMBER_PREFERENCE_KEY_BASE"
        private const val PUSHER_PREFERENCE_KEY_BASE = "PUSHER_PREFERENCE_KEY_BASE"
        private const val DEVICES_PREFERENCE_KEY_BASE = "DEVICES_PREFERENCE_KEY_BASE"
        private const val IGNORED_USER_KEY_BASE = "IGNORED_USER_KEY_BASE"
        private const val ADD_EMAIL_PREFERENCE_KEY = "ADD_EMAIL_PREFERENCE_KEY"
        private const val ADD_PHONE_NUMBER_PREFERENCE_KEY = "ADD_PHONE_NUMBER_PREFERENCE_KEY"
        private const val APP_INFO_LINK_PREFERENCE_KEY = "APP_INFO_LINK_PREFERENCE_KEY"

        private const val DUMMY_RULE = "DUMMY_RULE"
        private const val LABEL_UNAVAILABLE_DATA = "none"

        private const val REQUEST_E2E_FILE_REQUEST_CODE = 123
        private const val REQUEST_NEW_PHONE_NUMBER = 456
        private const val REQUEST_PHONEBOOK_COUNTRY = 789
        private const val REQUEST_LOCALE = 777
        private const val REQUEST_CALL_RINGTONE = 999

        // preference name <-> rule Id
        private var mPrefKeyToBingRuleId = mapOf(
                PreferencesManager.SETTINGS_ENABLE_ALL_NOTIF_PREFERENCE_KEY to BingRule.RULE_ID_DISABLE_ALL,
                PreferencesManager.SETTINGS_ENABLE_THIS_DEVICE_PREFERENCE_KEY to DUMMY_RULE,
                PreferencesManager.SETTINGS_TURN_SCREEN_ON_PREFERENCE_KEY to DUMMY_RULE
        )

        // static constructor
        fun newInstance(matrixId: String) = VectorSettingsPreferencesFragment()
                .withArgs {
                    //putString(ARG_MATRIX_ID, matrixId)
                }
    }

}