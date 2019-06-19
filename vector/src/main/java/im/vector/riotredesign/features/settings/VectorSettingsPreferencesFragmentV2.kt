package im.vector.riotredesign.features.settings

import android.os.Bundle
import im.vector.riotredesign.R
import im.vector.riotredesign.core.extensions.withArgs
import im.vector.riotredesign.core.platform.VectorPreferenceFragment

class VectorSettingsPreferencesFragmentV2 : VectorPreferenceFragment() {

    override var titleRes: Int = R.string.title_activity_settings

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.vector_settings_preferences_root)
    }


    companion object {
        fun newInstance() = VectorSettingsPreferencesFragmentV2()
                .withArgs {
                    //putString(ARG_MATRIX_ID, matrixId)
                }
    }

}