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

@file:Suppress("UNUSED_VARIABLE", "UNUSED_ANONYMOUS_PARAMETER", "UNUSED_PARAMETER")

package im.vector.app.features.settings

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.SwitchPreference
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.cache.DiskCache
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.yalantis.ucrop.UCrop
import im.vector.app.R
import im.vector.app.core.extensions.hideKeyboard
import im.vector.app.core.extensions.showPassword
import im.vector.app.core.intent.getFilenameFromUri
import im.vector.app.core.platform.SimpleTextWatcher
import im.vector.app.core.preference.UserAvatarPreference
import im.vector.app.core.preference.VectorPreference
import im.vector.app.core.preference.VectorSwitchPreference
import im.vector.app.core.utils.PERMISSIONS_FOR_TAKING_PHOTO
import im.vector.app.core.utils.PERMISSION_REQUEST_CODE_LAUNCH_CAMERA
import im.vector.app.core.utils.TextUtils
import im.vector.app.core.utils.allGranted
import im.vector.app.core.utils.checkPermissions
import im.vector.app.core.utils.getSizeOfFiles
import im.vector.app.core.utils.toast
import im.vector.app.features.MainActivity
import im.vector.app.features.MainActivityArgs
import im.vector.app.features.media.createUCropWithDefaultSettings
import im.vector.app.features.workers.signout.SignOutUiWorker
import im.vector.lib.multipicker.MultiPicker
import im.vector.lib.multipicker.entity.MultiPickerImageType
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.matrix.android.sdk.api.MatrixCallback
import org.matrix.android.sdk.api.NoOpMatrixCallback
import org.matrix.android.sdk.api.failure.isInvalidPassword
import org.matrix.android.sdk.api.session.integrationmanager.IntegrationManagerConfig
import org.matrix.android.sdk.api.session.integrationmanager.IntegrationManagerService
import org.matrix.android.sdk.rx.rx
import org.matrix.android.sdk.rx.unwrap
import java.io.File
import java.util.UUID

class VectorSettingsGeneralFragment : VectorSettingsBaseFragment() {

    override var titleRes = R.string.settings_general_title
    override val preferenceXmlRes = R.xml.vector_settings_general

    private var mDisplayedEmails = ArrayList<String>()
    private var mDisplayedPhoneNumber = ArrayList<String>()

    private var avatarCameraUri: Uri? = null

    private val mUserSettingsCategory by lazy {
        findPreference<PreferenceCategory>(VectorPreferences.SETTINGS_USER_SETTINGS_PREFERENCE_KEY)!!
    }
    private val mUserAvatarPreference by lazy {
        findPreference<UserAvatarPreference>(VectorPreferences.SETTINGS_PROFILE_PICTURE_PREFERENCE_KEY)!!
    }
    private val mDisplayNamePreference by lazy {
        findPreference<EditTextPreference>("SETTINGS_DISPLAY_NAME_PREFERENCE_KEY")!!
    }
    private val mPasswordPreference by lazy {
        findPreference<VectorPreference>(VectorPreferences.SETTINGS_CHANGE_PASSWORD_PREFERENCE_KEY)!!
    }
    private val mIdentityServerPreference by lazy {
        findPreference<VectorPreference>(VectorPreferences.SETTINGS_IDENTITY_SERVER_PREFERENCE_KEY)!!
    }

    // Local contacts
    private val mContactSettingsCategory by lazy {
        findPreference<PreferenceCategory>(VectorPreferences.SETTINGS_CONTACT_PREFERENCE_KEYS)!!
    }

    private val mContactPhonebookCountryPreference by lazy {
        findPreference<VectorPreference>(VectorPreferences.SETTINGS_CONTACTS_PHONEBOOK_COUNTRY_PREFERENCE_KEY)!!
    }

    private val integrationServiceListener = object : IntegrationManagerService.Listener {
        override fun onConfigurationChanged(configs: List<IntegrationManagerConfig>) {
            refreshIntegrationManagerSettings()
        }

        override fun onIsEnabledChanged(enabled: Boolean) {
            refreshIntegrationManagerSettings()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        observeUserAvatar()
        observeUserDisplayName()
    }

    private fun observeUserAvatar() {
        session.rx()
                .liveUser(session.myUserId)
                .unwrap()
                .distinctUntilChanged { user -> user.avatarUrl }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { mUserAvatarPreference.refreshAvatar(it) }
                .disposeOnDestroyView()
    }

    private fun observeUserDisplayName() {
        session.rx()
                .liveUser(session.myUserId)
                .unwrap()
                .map { it.displayName ?: "" }
                .distinctUntilChanged()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { displayName ->
                    mDisplayNamePreference.let {
                        it.summary = displayName
                        it.text = displayName
                    }
                }
                .disposeOnDestroyView()
    }

    override fun bindPref() {
        // Avatar
        mUserAvatarPreference.let {
            it.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                onUpdateAvatarClick()
                false
            }
        }

        // Display name
        mDisplayNamePreference.let {
            it.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
                newValue
                        ?.let { value -> (value as? String)?.trim() }
                        ?.let { value -> onDisplayNameChanged(value) }
                false
            }
        }

        // Password
        // Hide the preference if password can not be updated
        if (session.getHomeServerCapabilities().canChangePassword) {
            mPasswordPreference.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                onPasswordUpdateClick()
                false
            }
        } else {
            mPasswordPreference.isVisible = false
        }

        // Advanced settings

        // user account
        findPreference<VectorPreference>(VectorPreferences.SETTINGS_LOGGED_IN_PREFERENCE_KEY)!!
                .summary = session.myUserId

        // home server
        findPreference<VectorPreference>(VectorPreferences.SETTINGS_HOME_SERVER_PREFERENCE_KEY)!!
                .summary = session.sessionParams.homeServerUrl

        // Contacts
        setContactsPreferences()

        // clear cache
        findPreference<VectorPreference>(VectorPreferences.SETTINGS_CLEAR_CACHE_PREFERENCE_KEY)!!.let {
            /*
            TODO
            MXSession.getApplicationSizeCaches(activity, object : SimpleApiCallback<Long>() {
                override fun onSuccess(size: Long) {
                    if (null != activity) {
                        it.summary = TextUtils.formatFileSize(activity, size)
                    }
                }
            })
             */

            it.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                displayLoadingView()
                MainActivity.restartApp(requireActivity(), MainActivityArgs(clearCache = true))
                false
            }
        }

        (findPreference(VectorPreferences.SETTINGS_ALLOW_INTEGRATIONS_KEY) as? VectorSwitchPreference)?.let {
            it.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
                // Disable it while updating the state, will be re-enabled by the account data listener.
                it.isEnabled = false
                session.integrationManagerService().setIntegrationEnabled(newValue as Boolean, NoOpMatrixCallback())
                true
            }
        }

        // clear medias cache
        findPreference<VectorPreference>(VectorPreferences.SETTINGS_CLEAR_MEDIA_CACHE_PREFERENCE_KEY)!!.let {
            val size = getSizeOfFiles(File(requireContext().cacheDir, DiskCache.Factory.DEFAULT_DISK_CACHE_DIR)) + session.fileService().getCacheSize()

            it.summary = TextUtils.formatFileSize(requireContext(), size.toLong())

            it.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                GlobalScope.launch(Dispatchers.Main) {
                    // On UI Thread
                    displayLoadingView()

                    Glide.get(requireContext()).clearMemory()
                    session.fileService().clearCache()

                    var newSize: Int

                    withContext(Dispatchers.IO) {
                        // On BG thread
                        Glide.get(requireContext()).clearDiskCache()

                        newSize = getSizeOfFiles(File(requireContext().cacheDir, DiskCache.Factory.DEFAULT_DISK_CACHE_DIR))
                        newSize += session.fileService().getCacheSize()
                    }

                    it.summary = TextUtils.formatFileSize(requireContext(), newSize.toLong())

                    hideLoadingView()
                }

                false
            }
        }

        // Sign out
        findPreference<VectorPreference>("SETTINGS_SIGN_OUT_KEY")!!
                .onPreferenceClickListener = Preference.OnPreferenceClickListener {
            activity?.let {
                SignOutUiWorker(requireActivity())
                        .perform(requireContext())
            }

            false
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh identity server summary
        mIdentityServerPreference.summary = session.identityService().getCurrentIdentityServerUrl() ?: getString(R.string.identity_server_not_defined)
        refreshIntegrationManagerSettings()
        session.integrationManagerService().addListener(integrationServiceListener)
    }

    override fun onPause() {
        super.onPause()
        session.integrationManagerService().removeListener(integrationServiceListener)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (allGranted(grantResults)) {
            if (requestCode == PERMISSION_REQUEST_CODE_LAUNCH_CAMERA) {
                onAvatarTypeSelected(true)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                REQUEST_NEW_PHONE_NUMBER            -> refreshPhoneNumbersList()
                REQUEST_PHONEBOOK_COUNTRY           -> onPhonebookCountryUpdate(data)
                MultiPicker.REQUEST_CODE_TAKE_PHOTO -> {
                    avatarCameraUri?.let { uri ->
                        MultiPicker.get(MultiPicker.CAMERA)
                                .getTakenPhoto(requireContext(), requestCode, resultCode, uri)
                                ?.let {
                                    onAvatarSelected(it)
                                }
                    }
                }
                MultiPicker.REQUEST_CODE_PICK_IMAGE -> {
                    MultiPicker
                            .get(MultiPicker.IMAGE)
                            .getSelectedFiles(requireContext(), requestCode, resultCode, data)
                            .firstOrNull()?.let {
                                onAvatarSelected(it)
                            }
                }
                UCrop.REQUEST_CROP                  -> data?.let { onAvatarCropped(UCrop.getOutput(it)) }
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

    private fun refreshIntegrationManagerSettings() {
        val integrationAllowed = session.integrationManagerService().isIntegrationEnabled()
        (findPreference<SwitchPreference>(VectorPreferences.SETTINGS_ALLOW_INTEGRATIONS_KEY))!!.let {
            val savedListener = it.onPreferenceChangeListener
            it.onPreferenceChangeListener = null
            it.isChecked = integrationAllowed
            it.isEnabled = true
            it.onPreferenceChangeListener = savedListener
        }
        findPreference<VectorPreference>(VectorPreferences.SETTINGS_INTEGRATION_MANAGER_UI_URL_KEY)!!.let {
            if (integrationAllowed) {
                it.summary = session.integrationManagerService().getPreferredConfig().uiUrl
                it.isVisible = true
            } else {
                it.isVisible = false
            }
        }
    }

    /**
     * Update the avatar.
     */
    private fun onUpdateAvatarClick() {
        AlertDialog
                .Builder(requireContext())
                .setItems(arrayOf(
                        getString(R.string.attachment_type_camera),
                        getString(R.string.attachment_type_gallery)
                )) { dialog, which ->
                    dialog.cancel()
                    onAvatarTypeSelected(isCamera = (which == 0))
                }.show()
    }

    private fun onAvatarTypeSelected(isCamera: Boolean) {
        if (isCamera) {
            if (checkPermissions(PERMISSIONS_FOR_TAKING_PHOTO, this, PERMISSION_REQUEST_CODE_LAUNCH_CAMERA)) {
                avatarCameraUri = MultiPicker.get(MultiPicker.CAMERA).startWithExpectingFile(this)
            }
        } else {
            MultiPicker.get(MultiPicker.IMAGE).single().startWith(this)
        }
    }

    private fun onAvatarSelected(image: MultiPickerImageType) {
        val destinationFile = File(requireContext().cacheDir, "${image.displayName}_edited_image_${System.currentTimeMillis()}")
        val uri = image.contentUri
        createUCropWithDefaultSettings(requireContext(), uri, destinationFile.toUri(), image.displayName)
                .apply { withAspectRatio(1f, 1f) }
                .start(requireContext(), this)
    }

    private fun onAvatarCropped(uri: Uri?) {
        if (uri != null) {
            uploadAvatar(uri)
        } else {
            Toast.makeText(requireContext(), "Cannot retrieve cropped value", Toast.LENGTH_SHORT).show()
        }
    }

    private fun uploadAvatar(uri: Uri) {
        displayLoadingView()

        session.updateAvatar(session.myUserId, uri, getFilenameFromUri(context, uri) ?: UUID.randomUUID().toString(), object : MatrixCallback<Unit> {
            override fun onSuccess(data: Unit) {
                if (!isAdded) return
                onCommonDone(null)
            }

            override fun onFailure(failure: Throwable) {
                if (!isAdded) return
                onCommonDone(failure.localizedMessage)
            }
        })
    }

    // ==============================================================================================================
    // contacts management
    // ==============================================================================================================

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

    // ==============================================================================================================
    // Phone number management
    // ==============================================================================================================

    /**
     * Refresh phone number list
     */
    private fun refreshPhoneNumbersList() {
    }

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

            showPassword.setOnClickListener {
                passwordShown = !passwordShown

                oldPasswordText.showPassword(passwordShown)
                newPasswordText.showPassword(passwordShown)
                confirmNewPasswordText.showPassword(passwordShown)

                showPassword.setImageResource(if (passwordShown) R.drawable.ic_eye_closed else R.drawable.ic_eye)
            }

            val dialog = AlertDialog.Builder(activity)
                    .setView(view)
                    .setCancelable(false)
                    .setPositiveButton(R.string.settings_change_password, null)
                    .setNegativeButton(R.string.cancel, null)
                    .setOnDismissListener {
                        view.hideKeyboard()
                    }
                    .create()

            dialog.setOnShowListener {
                val updateButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                val cancelButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                updateButton.isEnabled = false

                fun updateUi() {
                    val oldPwd = oldPasswordText.text.toString()
                    val newPwd = newPasswordText.text.toString()
                    val newConfirmPwd = confirmNewPasswordText.text.toString()

                    updateButton.isEnabled = oldPwd.isNotEmpty() && newPwd.isNotEmpty() && newPwd == newConfirmPwd

                    if (newPwd.isNotEmpty() && newConfirmPwd.isNotEmpty() && newPwd != newConfirmPwd) {
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
                        cancelButton.isEnabled = false
                    } else {
                        showPassword.isEnabled = true
                        oldPasswordText.isEnabled = true
                        newPasswordText.isEnabled = true
                        confirmNewPasswordText.isEnabled = true
                        changePasswordLoader.isVisible = false
                        updateButton.isEnabled = true
                        cancelButton.isEnabled = true
                    }
                }

                updateButton.setOnClickListener {
                    if (passwordShown) {
                        // Hide passwords during processing
                        showPassword.performClick()
                    }

                    view.hideKeyboard()

                    val oldPwd = oldPasswordText.text.toString()
                    val newPwd = newPasswordText.text.toString()

                    showPasswordLoadingView(true)
                    session.changePassword(oldPwd, newPwd, object : MatrixCallback<Unit> {
                        override fun onSuccess(data: Unit) {
                            if (!isAdded) {
                                return
                            }
                            showPasswordLoadingView(false)
                            dialog.dismiss()
                            activity.toast(R.string.settings_password_updated)
                        }

                        override fun onFailure(failure: Throwable) {
                            if (!isAdded) {
                                return
                            }
                            showPasswordLoadingView(false)
                            if (failure.isInvalidPassword()) {
                                oldPasswordTil.error = getString(R.string.settings_fail_to_update_password_invalid_current_password)
                            } else {
                                oldPasswordTil.error = getString(R.string.settings_fail_to_update_password)
                            }
                        }
                    })
                }
            }
            dialog.show()
        }
    }

    /**
     * Update the displayname.
     */
    private fun onDisplayNameChanged(value: String) {
        val currentDisplayName = session.getUser(session.myUserId)?.displayName ?: ""
        if (currentDisplayName != value) {
            displayLoadingView()

            session.setDisplayName(session.myUserId, value, object : MatrixCallback<Unit> {
                override fun onSuccess(data: Unit) {
                    if (!isAdded) return
                    // refresh the settings value
                    mDisplayNamePreference.summary = value
                    mDisplayNamePreference.text = value
                    onCommonDone(null)
                }

                override fun onFailure(failure: Throwable) {
                    if (!isAdded) return
                    onCommonDone(failure.localizedMessage)
                }
            })
        }
    }

    companion object {
        private const val REQUEST_NEW_PHONE_NUMBER = 456
        private const val REQUEST_PHONEBOOK_COUNTRY = 789
    }
}
