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

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.text.Editable
import android.util.Patterns
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.cache.DiskCache
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import im.vector.riotx.R
import im.vector.riotx.core.extensions.showPassword
import im.vector.riotx.core.platform.SimpleTextWatcher
import im.vector.riotx.core.preference.UserAvatarPreference
import im.vector.riotx.core.preference.VectorPreference
import im.vector.riotx.core.utils.*
import im.vector.riotx.features.MainActivity
import im.vector.riotx.features.themes.ThemeUtils
import im.vector.riotx.features.workers.signout.SignOutUiWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.*

class VectorSettingsGeneralFragment : VectorSettingsBaseFragment() {

    override var titleRes = R.string.settings_general_title
    override val preferenceXmlRes = R.xml.vector_settings_general


    private var mDisplayedEmails = ArrayList<String>()
    private var mDisplayedPhoneNumber = ArrayList<String>()

    private val mUserSettingsCategory by lazy {
        findPreference<PreferenceCategory>(VectorPreferences.SETTINGS_USER_SETTINGS_PREFERENCE_KEY)!!
    }
    private val mUserAvatarPreference by lazy {
        findPreference<UserAvatarPreference>(VectorPreferences.SETTINGS_PROFILE_PICTURE_PREFERENCE_KEY)!!
    }
    private val mDisplayNamePreference by lazy {
        findPreference<EditTextPreference>(VectorPreferences.SETTINGS_DISPLAY_NAME_PREFERENCE_KEY)!!
    }
    private val mPasswordPreference by lazy {
        findPreference<VectorPreference>(VectorPreferences.SETTINGS_CHANGE_PASSWORD_PREFERENCE_KEY)!!
    }

    // Local contacts
    private val mContactSettingsCategory by lazy {
        findPreference<PreferenceCategory>(VectorPreferences.SETTINGS_CONTACT_PREFERENCE_KEYS)!!
    }

    private val mContactPhonebookCountryPreference by lazy {
        findPreference<VectorPreference>(VectorPreferences.SETTINGS_CONTACTS_PHONEBOOK_COUNTRY_PREFERENCE_KEY)!!
    }


    override fun bindPref() {
        // Avatar
        mUserAvatarPreference.let {
            it.setSession(session)
            it.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                onUpdateAvatarClick()
                false
            }
        }

        // Display name
        mDisplayNamePreference.let {
            it.summary = session.getUser(session.myUserId)?.displayName ?: ""
            it.text = it.summary.toString()
            it.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
                onDisplayNameClick(newValue?.let { (it as String).trim() })
                false
            }
        }

        // Password
        mPasswordPreference.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            notImplemented()
            // onPasswordUpdateClick()
            false
        }

        // Add Email
        findPreference<EditTextPreference>(ADD_EMAIL_PREFERENCE_KEY)!!.let {
            // It does not work on XML, do it here
            it.icon = activity?.let {
                ThemeUtils.tintDrawable(it,
                        ContextCompat.getDrawable(it, R.drawable.ic_add_black)!!, R.attr.vctr_settings_icon_tint_color)
            }

            // Unfortunately, this is not supported in lib v7
            // it.editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS

            it.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
                notImplemented()
                //addEmail((newValue as String).trim())
                false
            }
        }

        // Add phone number
        findPreference<VectorPreference>(ADD_PHONE_NUMBER_PREFERENCE_KEY)!!.let {
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

        // Advanced settings

        // user account
        findPreference<VectorPreference>(VectorPreferences.SETTINGS_LOGGED_IN_PREFERENCE_KEY)!!
                .summary = session.myUserId

        // home server
        findPreference<VectorPreference>(VectorPreferences.SETTINGS_HOME_SERVER_PREFERENCE_KEY)!!
                .summary = session.sessionParams.homeServerConnectionConfig.homeServerUri.toString()

        // identity server
        findPreference<VectorPreference>(VectorPreferences.SETTINGS_IDENTITY_SERVER_PREFERENCE_KEY)!!
                .summary = session.sessionParams.homeServerConnectionConfig.identityServerUri.toString()


        refreshEmailsList()
        refreshPhoneNumbersList()
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
                MainActivity.restartApp(activity!!, clearCache = true, clearCredentials = false)
                false
            }
        }

        // clear medias cache
        findPreference<VectorPreference>(VectorPreferences.SETTINGS_CLEAR_MEDIA_CACHE_PREFERENCE_KEY)!!.let {
            val size = getSizeOfFiles(requireContext(),
                    File(requireContext().cacheDir, DiskCache.Factory.DEFAULT_DISK_CACHE_DIR))

            it.summary = TextUtils.formatFileSize(requireContext(), size.toLong())

            it.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                GlobalScope.launch(Dispatchers.Main) {
                    // On UI Thread
                    displayLoadingView()

                    Glide.get(requireContext()).clearMemory()

                    var newSize = 0

                    withContext(Dispatchers.IO) {
                        // On BG thread
                        Glide.get(requireContext()).clearDiskCache()

                        newSize = getSizeOfFiles(requireContext(),
                                File(requireContext().cacheDir, DiskCache.Factory.DEFAULT_DISK_CACHE_DIR))
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


        // Deactivate account section

        // deactivate account
        findPreference<VectorPreference>(VectorPreferences.SETTINGS_DEACTIVATE_ACCOUNT_KEY)!!
                .onPreferenceClickListener = Preference.OnPreferenceClickListener {
            activity?.let {
                notImplemented()
                // TODO startActivity(DeactivateAccountActivity.getIntent(it))
            }

            false
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (allGranted(grantResults)) {
            if (requestCode == PERMISSION_REQUEST_CODE_LAUNCH_CAMERA) {
                changeAvatar()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                REQUEST_NEW_PHONE_NUMBER  -> refreshPhoneNumbersList()
                REQUEST_PHONEBOOK_COUNTRY -> onPhonebookCountryUpdate(data)
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
                    val preference = mUserSettingsCategory.findPreference<VectorPreference>(EMAIL_PREFERENCE_KEY_BASE + index)

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

            val addEmailBtn = mUserSettingsCategory.findPreference<VectorPreference>(ADD_EMAIL_PREFERENCE_KEY) ?: return

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
     * Attempt to add a new email to the account
     *
     * @param email the email to add.
     */
    private fun addEmail(email: String) {
        // check first if the email syntax is valid
        // if email is null , then also its invalid email
        if (email.isBlank() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
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
                        putString(VectorPreferences.SETTINGS_DISPLAY_NAME_PREFERENCE_KEY, value)
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

    companion object {
        private const val ADD_EMAIL_PREFERENCE_KEY = "ADD_EMAIL_PREFERENCE_KEY"
        private const val ADD_PHONE_NUMBER_PREFERENCE_KEY = "ADD_PHONE_NUMBER_PREFERENCE_KEY"

        private const val EMAIL_PREFERENCE_KEY_BASE = "EMAIL_PREFERENCE_KEY_BASE"
        private const val PHONE_NUMBER_PREFERENCE_KEY_BASE = "PHONE_NUMBER_PREFERENCE_KEY_BASE"

        private const val REQUEST_NEW_PHONE_NUMBER = 456
        private const val REQUEST_PHONEBOOK_COUNTRY = 789
    }
}