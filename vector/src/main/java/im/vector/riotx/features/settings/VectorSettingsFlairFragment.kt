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

import androidx.preference.PreferenceCategory
import im.vector.riotx.R
import im.vector.riotx.core.preference.ProgressBarPreference

class VectorSettingsFlairFragment : VectorSettingsBaseFragment() {

    override var titleRes = R.string.settings_flair
    override val preferenceXmlRes = R.xml.vector_settings_flair

    // current publicised group list
    private var mPublicisedGroups: MutableSet<String>? = null

    // Group Flairs
    private val mGroupsFlairCategory by lazy {
        findPreference<PreferenceCategory>(VectorPreferences.SETTINGS_GROUPS_FLAIR_KEY)!!
    }

    override fun bindPref() {
        // Flair
        refreshGroupFlairsList()
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
        }
    }

}