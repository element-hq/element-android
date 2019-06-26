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

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.AsyncTask
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.content.edit
import androidx.preference.Preference
import androidx.preference.PreferenceManager
import im.vector.riotredesign.R
import im.vector.riotredesign.core.preference.BingRule
import im.vector.riotredesign.core.preference.VectorPreference
import java.lang.ref.WeakReference

// TODO Remove
class VectorSettingsPreferencesFragment {

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


    // TODO private var mDisplayedPushers = ArrayList<Pusher>()

    private var interactionListener: VectorSettingsFragmentInteractionListener? = null


    fun bindPref() {
        // push rules

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



    }

    fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        // if the user toggles the contacts book permission
        /* TODO
        if (TextUtils.equals(key, ContactsManager.CONTACTS_BOOK_ACCESS_KEY)) {
            // reset the current snapshot
            ContactsManager.getInstance().clearSnapshot()
        }
        */
    }

    /* TODO
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
    */

//    override fun onResume() {
//        super.onResume()
//
//
//        /* TODO
//        if (session.isAlive) {
//            val context = activity?.applicationContext
//
//            session.dataHandler.addListener(mEventsListener)
//
//            Matrix.getInstance(context)?.addNetworkEventListener(mNetworkListener)
//
//            session.myUser.refreshThirdPartyIdentifiers(object : SimpleApiCallback<Unit>() {
//                override fun onSuccess(info: Void?) {
//                    // ensure that the activity still exists
//                    // and the result is called in the right thread
//                    activity?.runOnUiThread {
//                        refreshEmailsList()
//                        refreshPhoneNumbersList()
//                    }
//                }
//            })
//
//            Matrix.getInstance(context)?.pushManager?.refreshPushersList(Matrix.getInstance(context)?.sessions, object : SimpleApiCallback<Unit>(activity) {
//                override fun onSuccess(info: Void?) {
//                    refreshPushersList()
//                }
//            })
//
//            PreferenceManager.getDefaultSharedPreferences(context).registerOnSharedPreferenceChangeListener(this)
//
//            // refresh anything else
//            refreshPreferences()
//            refreshNotificationPrivacy()
//            refreshDisplay()
//            refreshBackgroundSyncPrefs()
//        }
//        */
//
//        interactionListener?.requestedKeyToHighlight()?.let { key ->
//            interactionListener?.requestHighlightPreferenceKeyOnResume(null)
//            val preference = findPreference(key)
//            (preference as? VectorPreference)?.isHighlighted = true
//        }
//    }

//    override fun onPause() {
//        super.onPause()
//
//        val context = activity?.applicationContext
//
//        /* TODO
//        if (session.isAlive) {
//            session.dataHandler.removeListener(mEventsListener)
//            Matrix.getInstance(context)?.removeNetworkEventListener(mNetworkListener)
//        }
//        */
//
//        PreferenceManager.getDefaultSharedPreferences(context).unregisterOnSharedPreferenceChangeListener(this)
//    }

    //==============================================================================================================
    // Display methods
    //==============================================================================================================

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


    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
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
        //PreferenceManager.getDefaultSharedPreferences(activity).edit {
        //    putString(PreferencesManager.SETTINGS_DISPLAY_NAME_PREFERENCE_KEY, "TODO") //session.myUser.displayname)
//
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
  //      }
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
//    private fun secondsToText(seconds: Int): String {
//        return if (seconds > 1) {
//            seconds.toString() + " " + getString(R.string.settings_seconds)
//        } else {
//            seconds.toString() + " " + getString(R.string.settings_second)
//        }
//    }



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

//        // static constructor
//        fun newInstance(matrixId: String) = VectorSettingsPreferencesFragment()
//                .withArgs {
//                    //putString(ARG_MATRIX_ID, matrixId)
//                }
    }

}