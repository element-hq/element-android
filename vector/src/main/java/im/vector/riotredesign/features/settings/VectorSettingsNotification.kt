package im.vector.riotredesign.features.settings

import android.os.Bundle
import im.vector.riotredesign.R
import im.vector.riotredesign.core.platform.VectorBaseActivity
import im.vector.riotredesign.core.platform.VectorPreferenceFragment


class VectorSettingsNotificationPreferenceFragment : VectorPreferenceFragment() {

    override var titleRes: Int = R.string.settings_notifications

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.vector_settings_notifications)
    }

}