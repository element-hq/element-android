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
import android.content.Context
import android.content.Intent
import android.text.TextUtils
import android.widget.CheckedTextView
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.preference.Preference
import androidx.preference.SwitchPreference
import im.vector.riotx.R
import im.vector.riotx.core.di.ScreenComponent
import im.vector.riotx.core.preference.VectorListPreference
import im.vector.riotx.core.preference.VectorPreference
import im.vector.riotx.features.configuration.VectorConfiguration
import im.vector.riotx.features.themes.ThemeUtils
import javax.inject.Inject

class VectorSettingsPreferencesFragment : VectorSettingsBaseFragment() {

    override var titleRes = R.string.settings_preferences
    override val preferenceXmlRes = R.xml.vector_settings_preferences

    private val selectedLanguagePreference by lazy {
        findPreference<VectorPreference>(VectorPreferences.SETTINGS_INTERFACE_LANGUAGE_PREFERENCE_KEY)!!
    }
    private val textSizePreference by lazy {
        findPreference<VectorPreference>(VectorPreferences.SETTINGS_INTERFACE_TEXT_SIZE_KEY)!!
    }

    @Inject lateinit var vectorConfiguration: VectorConfiguration
    @Inject lateinit var vectorPreferences: VectorPreferences

    override fun injectWith(injector: ScreenComponent) {
        injector.inject(this)
    }


    override fun bindPref() {
        // user interface preferences
        setUserInterfacePreferences()

        // Themes
        findPreference<VectorListPreference>(ThemeUtils.APPLICATION_THEME_KEY)!!
                .onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
            if (newValue is String) {
                vectorConfiguration.updateApplicationTheme(newValue)
                // Restart the Activity
                activity?.let {
                    // Note: recreate does not apply the color correctly
                    it.startActivity(it.intent)
                    it.finish()
                }
                true
            } else {
                false
            }
        }

        // Url preview
        findPreference<SwitchPreference>(VectorPreferences.SETTINGS_SHOW_URL_PREVIEW_KEY)!!.let {
            /*
            TODO
            it.isChecked = session.isURLPreviewEnabled

            it.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
                if (null != newValue && newValue as Boolean != session.isURLPreviewEnabled) {
                    displayLoadingView()
                    session.setURLPreviewStatus(newValue, object : MatrixCallback<Unit> {
                        override fun onSuccess(info: Void?) {
                            it.isChecked = session.isURLPreviewEnabled
                            hideLoadingView()
                        }

                        private fun onError(errorMessage: String) {
                            activity?.toast(errorMessage)

                            onSuccess(null)
                        }

                        override fun onNetworkError(e: Exception) {
                            onError(e.localizedMessage)
                        }

                        override fun onMatrixError(e: MatrixError) {
                            onError(e.localizedMessage)
                        }

                        override fun onUnexpectedError(e: Exception) {
                            onError(e.localizedMessage)
                        }
                    })
                }

                false
            }
            */
        }

        // update keep medias period
        findPreference<VectorPreference>(VectorPreferences.SETTINGS_MEDIA_SAVING_PERIOD_KEY)!!.let {
            it.summary = vectorPreferences.getSelectedMediasSavingPeriodString()

            it.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                context?.let { context: Context ->
                    AlertDialog.Builder(context)
                            .setSingleChoiceItems(R.array.media_saving_choice,
                                    vectorPreferences.getSelectedMediasSavingPeriod()) { d, n ->
                                vectorPreferences.setSelectedMediasSavingPeriod(n)
                                d.cancel()

                                it.summary = vectorPreferences.getSelectedMediasSavingPeriodString()
                            }
                            .show()
                }

                false
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                REQUEST_LOCALE -> {
                    activity?.let {
                        startActivity(it.intent)
                        it.finish()
                    }
                }
            }
        }
    }


    //==============================================================================================================
    // user interface management
    //==============================================================================================================

    private fun setUserInterfacePreferences() {
        // Selected language
        selectedLanguagePreference.summary = VectorLocale.localeToLocalisedString(VectorLocale.applicationLocale)

        selectedLanguagePreference.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            notImplemented()
            // TODO startActivityForResult(LanguagePickerActivity.getIntent(activity), REQUEST_LOCALE)
            true
        }

        // Text size
        textSizePreference.summary = FontScale.getFontScaleDescription(activity!!)

        textSizePreference.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            activity?.let { displayTextSizeSelection(it) }
            true
        }
    }

    private fun displayTextSizeSelection(activity: Activity) {
        val inflater = activity.layoutInflater
        val layout = inflater.inflate(R.layout.dialog_select_text_size, null)

        val dialog = AlertDialog.Builder(activity)
                .setTitle(R.string.font_size)
                .setView(layout)
                .setPositiveButton(R.string.ok, null)
                .setNegativeButton(R.string.cancel, null)
                .show()

        val linearLayout = layout.findViewById<LinearLayout>(R.id.text_selection_group_view)

        val childCount = linearLayout.childCount

        val scaleText = FontScale.getFontScaleDescription(activity)

        for (i in 0 until childCount) {
            val v = linearLayout.getChildAt(i)

            if (v is CheckedTextView) {
                v.isChecked = TextUtils.equals(v.text, scaleText)

                v.setOnClickListener {
                    dialog.dismiss()
                    FontScale.updateFontScale(activity, v.text.toString())
                    activity.startActivity(activity.intent)
                    activity.finish()
                }
            }
        }
    }

    companion object {
        private const val REQUEST_LOCALE = 777
    }

}