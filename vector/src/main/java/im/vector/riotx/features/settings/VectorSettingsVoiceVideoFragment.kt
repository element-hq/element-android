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
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import androidx.preference.Preference
import androidx.preference.SwitchPreference
import im.vector.riotx.R
import im.vector.riotx.core.preference.VectorPreference
import im.vector.riotx.core.utils.getCallRingtoneName
import im.vector.riotx.core.utils.getCallRingtoneUri
import im.vector.riotx.core.utils.setCallRingtoneUri
import im.vector.riotx.core.utils.setUseRiotDefaultRingtone

class VectorSettingsVoiceVideoFragment : VectorSettingsBaseFragment() {

    override var titleRes = R.string.preference_voice_and_video
    override val preferenceXmlRes = R.xml.vector_settings_voice_video

    private val mUseRiotCallRingtonePreference by lazy {
        findPreference<SwitchPreference>(VectorPreferences.SETTINGS_CALL_RINGTONE_USE_RIOT_PREFERENCE_KEY)!!
    }
    private val mCallRingtonePreference by lazy {
        findPreference<VectorPreference>(VectorPreferences.SETTINGS_CALL_RINGTONE_URI_PREFERENCE_KEY)!!
    }

    override fun bindPref() {
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
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                REQUEST_CALL_RINGTONE -> {
                    val callRingtoneUri: Uri? = data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
                    val thisActivity = activity
                    if (callRingtoneUri != null && thisActivity != null) {
                        setCallRingtoneUri(thisActivity, callRingtoneUri)
                        mCallRingtonePreference.summary = getCallRingtoneName(thisActivity)
                    }
                }
            }
        }
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

    companion object {
        private const val REQUEST_CALL_RINGTONE = 999
    }

}