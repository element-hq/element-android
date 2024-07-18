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

package im.vector.app.features.settings

import android.app.Activity
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.SwitchPreference
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.R
import im.vector.app.core.extensions.registerStartForActivityResult
import im.vector.app.core.preference.VectorPreference
import im.vector.app.core.utils.RingtoneUtils
import im.vector.app.features.analytics.plan.MobileScreen
import im.vector.lib.core.utils.compat.getParcelableExtraCompat
import im.vector.lib.strings.CommonStrings
import javax.inject.Inject

@AndroidEntryPoint
class VectorSettingsVoiceVideoFragment : VectorSettingsBaseFragment() {

    @Inject lateinit var ringtoneUtils: RingtoneUtils

    override var titleRes = CommonStrings.preference_voice_and_video
    override val preferenceXmlRes = R.xml.vector_settings_voice_video

    private val mUseRiotCallRingtonePreference by lazy {
        findPreference<SwitchPreference>(VectorPreferences.SETTINGS_CALL_RINGTONE_USE_RIOT_PREFERENCE_KEY)!!
    }
    private val mCallRingtonePreference by lazy {
        findPreference<VectorPreference>(VectorPreferences.SETTINGS_CALL_RINGTONE_URI_PREFERENCE_KEY)!!
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        analyticsScreenName = MobileScreen.ScreenName.SettingsVoiceVideo
    }

    override fun bindPref() {
        // Incoming call sounds
        mUseRiotCallRingtonePreference.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            ringtoneUtils.setUseRiotDefaultRingtone(mUseRiotCallRingtonePreference.isChecked)
            false
        }

        mCallRingtonePreference.let {
            it.summary = ringtoneUtils.getCallRingtoneName()
            it.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                displayRingtonePicker()
                false
            }
        }
    }

    private val ringtoneStartForActivityResult = registerStartForActivityResult { activityResult ->
        if (activityResult.resultCode == Activity.RESULT_OK) {
            val callRingtoneUri: Uri? = activityResult.data?.getParcelableExtraCompat(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            if (callRingtoneUri != null) {
                ringtoneUtils.setCallRingtoneUri(callRingtoneUri)
                mCallRingtonePreference.summary = ringtoneUtils.getCallRingtoneName()
            }
        }
    }

    private fun displayRingtonePicker() {
        val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
            putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, getString(CommonStrings.settings_call_ringtone_dialog_title))
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
            putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_RINGTONE)
            putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, ringtoneUtils.getCallRingtoneUri())
        }
        ringtoneStartForActivityResult.launch(intent)
    }
}
