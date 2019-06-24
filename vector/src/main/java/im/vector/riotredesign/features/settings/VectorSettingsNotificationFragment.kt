package im.vector.riotredesign.features.settings

import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.SwitchPreference
import im.vector.matrix.android.api.Matrix
import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.api.session.pushers.PushersService
import im.vector.riotredesign.R
import im.vector.riotredesign.core.platform.VectorPreferenceFragment
import im.vector.riotredesign.core.pushers.PushersManager
import im.vector.riotredesign.push.fcm.FcmHelper
import org.koin.android.ext.android.get
import org.koin.android.ext.android.inject


class VectorSettingsNotificationPreferenceFragment : VectorPreferenceFragment() {

    override var titleRes: Int = R.string.settings_notifications

    val pushManager: PushersManager by inject()


    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.vector_settings_notifications)
    }

    override fun onResume() {
        super.onResume()
        Matrix.getInstance().currentSession?.refreshPushers()
    }

    override fun onPreferenceTreeClick(preference: Preference?): Boolean {
        if (preference?.key == PreferencesManager.SETTINGS_ENABLE_THIS_DEVICE_PREFERENCE_KEY) {
            val switchPref = preference as SwitchPreference
            if (switchPref.isChecked) {
                FcmHelper.getFcmToken(requireContext())?.let {
                    if (PreferencesManager.areNotificationEnabledForDevice(requireContext())) {
                        pushManager.registerPusherWithFcmKey(it)
                    }
                }
            } else {
                FcmHelper.getFcmToken(requireContext())?.let {
                    pushManager.unregisterPusher(it, object : MatrixCallback<Unit> {
                        override fun onSuccess(data: Unit) {
                            super.onSuccess(data)
                        }

                        override fun onFailure(failure: Throwable) {
                            super.onFailure(failure)
                        }
                    })
                }
            }
        }
        return super.onPreferenceTreeClick(preference)
    }
}