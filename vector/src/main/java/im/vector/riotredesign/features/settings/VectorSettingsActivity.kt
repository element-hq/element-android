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

import android.content.Context
import android.content.Intent
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import im.vector.matrix.android.api.session.Session
import im.vector.riotredesign.R
import im.vector.riotredesign.core.di.ScreenComponent
import im.vector.riotredesign.core.platform.VectorBaseActivity
import kotlinx.android.synthetic.main.activity_vector_settings.*
import javax.inject.Inject

/**
 * Displays the client settings.
 */
class VectorSettingsActivity : VectorBaseActivity(),
        PreferenceFragmentCompat.OnPreferenceStartFragmentCallback,
        FragmentManager.OnBackStackChangedListener,
        VectorSettingsFragmentInteractionListener {

    private lateinit var vectorSettingsPreferencesFragment: VectorSettingsPreferencesFragment

    override fun getLayoutRes() = R.layout.activity_vector_settings

    override fun getTitleRes() = R.string.title_activity_settings

    private var keyToHighlight: String? = null

    @Inject lateinit var session: Session

    override fun injectWith(injector: ScreenComponent) {
        injector.inject(this)
    }

    override fun initUiAndData() {
        configureToolbar(settingsToolbar)

        if (isFirstCreation()) {
            vectorSettingsPreferencesFragment = VectorSettingsPreferencesFragment.newInstance(session.sessionParams.credentials.userId)
            // display the fragment
            supportFragmentManager.beginTransaction()
                    .replace(R.id.vector_settings_page, vectorSettingsPreferencesFragment, FRAGMENT_TAG)
                    .commit()
        } else {
            vectorSettingsPreferencesFragment = supportFragmentManager.findFragmentByTag(FRAGMENT_TAG) as VectorSettingsPreferencesFragment
        }


        supportFragmentManager.addOnBackStackChangedListener(this)

    }

    override fun onDestroy() {
        supportFragmentManager.removeOnBackStackChangedListener(this)
        super.onDestroy()
    }

    override fun onBackStackChanged() {
        if (0 == supportFragmentManager.backStackEntryCount) {
            supportActionBar?.title = getString(getTitleRes())
        }
    }

    override fun onPreferenceStartFragment(caller: PreferenceFragmentCompat?, pref: Preference?): Boolean {
        var oFragment: Fragment? = null

        if (PreferencesManager.SETTINGS_NOTIFICATION_TROUBLESHOOT_PREFERENCE_KEY == pref?.key) {
            oFragment = VectorSettingsNotificationsTroubleshootFragment.newInstance(session.sessionParams.credentials.userId)
        } else if (PreferencesManager.SETTINGS_NOTIFICATION_ADVANCED_PREFERENCE_KEY == pref?.key) {
            oFragment = VectorSettingsAdvancedNotificationPreferenceFragment.newInstance(session.sessionParams.credentials.userId)
        }

        if (oFragment != null) {
            oFragment.setTargetFragment(caller, 0)
            // Replace the existing Fragment with the new Fragment
            supportFragmentManager.beginTransaction()
                    .setCustomAnimations(R.anim.anim_slide_in_bottom, R.anim.anim_slide_out_bottom,
                            R.anim.anim_slide_in_bottom, R.anim.anim_slide_out_bottom)
                    .replace(R.id.vector_settings_page, oFragment, pref?.title.toString())
                    .addToBackStack(null)
                    .commit()
            return true
        }
        return false
    }


    override fun requestHighlightPreferenceKeyOnResume(key: String?) {
        keyToHighlight = key
    }

    override fun requestedKeyToHighlight(): String? {
        return keyToHighlight
    }

    companion object {
        fun getIntent(context: Context, userId: String) = Intent(context, VectorSettingsActivity::class.java)
                .apply {
                    //putExtra(MXCActionBarActivity.EXTRA_MATRIX_ID, userId)
                }

        private const val FRAGMENT_TAG = "VectorSettingsPreferencesFragment"
    }
}
