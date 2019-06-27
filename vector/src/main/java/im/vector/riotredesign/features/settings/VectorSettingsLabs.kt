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

package im.vector.riotredesign.features.settings

import android.text.TextUtils
import androidx.appcompat.app.AlertDialog
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.SwitchPreference
import im.vector.riotredesign.R

class VectorSettingsLabs : VectorSettingsBaseFragment() {

    override var titleRes = R.string.room_settings_labs_pref_title
    override val preferenceXmlRes = R.xml.vector_settings_labs

    private val mLabsCategory by lazy {
        findPreference(PreferencesManager.SETTINGS_LABS_PREFERENCE_KEY) as PreferenceCategory
    }

    override fun bindPref() {
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
    }

}