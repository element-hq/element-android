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

import androidx.appcompat.app.AlertDialog
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import im.vector.riotx.R
import im.vector.riotx.core.preference.VectorPreference
import java.util.ArrayList
import kotlin.Comparator

class VectorSettingsIgnoredUsersFragment : VectorSettingsBaseFragment() {

    override var titleRes = R.string.settings_ignored_users
    override val preferenceXmlRes = R.xml.vector_settings_ignored_users

    // displayed the ignored users list
    private val mIgnoredUserSettingsCategoryDivider by lazy {
        findPreference<VectorPreference>(VectorPreferences.SETTINGS_IGNORE_USERS_DIVIDER_PREFERENCE_KEY)!!
    }
    private val mIgnoredUserSettingsCategory by lazy {
        findPreference<PreferenceCategory>(VectorPreferences.SETTINGS_IGNORED_USERS_PREFERENCE_KEY)!!
    }

    override fun bindPref() {
        // Ignore users
        refreshIgnoredUsersList()
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

    companion object {
        private const val IGNORED_USER_KEY_BASE = "IGNORED_USER_KEY_BASE"
    }
}