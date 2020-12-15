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
import android.content.Context
import android.widget.CheckedTextView
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.preference.Preference
import im.vector.app.R
import im.vector.app.core.extensions.restart
import im.vector.app.core.preference.VectorListPreference
import im.vector.app.core.preference.VectorPreference
import im.vector.app.core.preference.VectorSwitchPreference
import im.vector.app.features.configuration.VectorConfiguration
import im.vector.app.features.themes.BubbleThemeUtils
import im.vector.app.features.themes.ThemeUtils
import javax.inject.Inject

class VectorSettingsPreferencesFragment @Inject constructor(
        private val vectorConfiguration: VectorConfiguration,
        private val vectorPreferences: VectorPreferences
) : VectorSettingsBaseFragment() {

    override var titleRes = R.string.settings_preferences
    override val preferenceXmlRes = R.xml.vector_settings_preferences

    private var bubbleTimeLocationPref: VectorListPreference? = null
    private var alwaysShowTimestampsPref: VectorSwitchPreference? = null

    private val selectedLanguagePreference by lazy {
        findPreference<VectorPreference>(VectorPreferences.SETTINGS_INTERFACE_LANGUAGE_PREFERENCE_KEY)!!
    }
    private val textSizePreference by lazy {
        findPreference<VectorPreference>(VectorPreferences.SETTINGS_INTERFACE_TEXT_SIZE_KEY)!!
    }

    override fun bindPref() {
        // user interface preferences
        setUserInterfacePreferences()

        // Themes
        val lightThemePref = findPreference<VectorListPreference>(ThemeUtils.APPLICATION_THEME_KEY)!!
        val darkThemePref = findPreference<VectorListPreference>(ThemeUtils.APPLICATION_DARK_THEME_KEY)!!
        lightThemePref.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
            if (newValue is String) {
                ThemeUtils.setApplicationLightTheme(requireContext().applicationContext, newValue)
                if (!ThemeUtils.shouldUseDarkTheme(requireContext())) {
                    // Restart the Activity
                    activity?.restart()
                }
                true
            } else {
                false
            }
        }
        if (ThemeUtils.darkThemePossible(requireContext())) {
            lightThemePref.title = getString(R.string.settings_light_theme)
            darkThemePref.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
                if (newValue is String) {
                    ThemeUtils.setApplicationDarkTheme(requireContext().applicationContext, newValue)
                    if (ThemeUtils.shouldUseDarkTheme(requireContext())) {
                        // Restart the Activity
                        activity?.restart()
                    }
                    true
                } else {
                    false
                }
            }
        } else {
            lightThemePref.title = getString(R.string.settings_theme)
            darkThemePref.parent?.removePreference(darkThemePref)
        }

        val bubbleStylePreference = findPreference<VectorListPreference>(BubbleThemeUtils.BUBBLE_STYLE_KEY)
        bubbleStylePreference!!.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
            BubbleThemeUtils.invalidateBubbleStyle()
            updateBubbleDependencies(
                    bubbleStyle = newValue as String,
                    bubbleTimeLocation = bubbleTimeLocationPref!!.value
            )
            true
        }

        bubbleTimeLocationPref = findPreference<VectorListPreference>(BubbleThemeUtils.BUBBLE_TIME_LOCATION_KEY)
        alwaysShowTimestampsPref = findPreference<VectorSwitchPreference>(VectorPreferences.SETTINGS_ALWAYS_SHOW_TIMESTAMPS_KEY)
        bubbleTimeLocationPref!!.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
            BubbleThemeUtils.invalidateBubbleStyle()
            updateBubbleDependencies(
                    bubbleStyle = bubbleStylePreference.value,
                    bubbleTimeLocation = newValue as String
            )
            true
        }
        updateBubbleDependencies(
                bubbleStyle = bubbleStylePreference.value,
                bubbleTimeLocation = bubbleTimeLocationPref!!.value
        )

        // Url preview
        /*
        TODO Note: we keep the setting client side for now
        findPreference<SwitchPreference>(VectorPreferences.SETTINGS_SHOW_URL_PREVIEW_KEY)!!.let {
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
        }
        */

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

    // ==============================================================================================================
    // user interface management
    // ==============================================================================================================

    private fun setUserInterfacePreferences() {
        // Selected language
        selectedLanguagePreference.summary = VectorLocale.localeToLocalisedString(VectorLocale.applicationLocale)

        // Text size
        textSizePreference.summary = getString(FontScale.getFontScaleValue(requireActivity()).nameResId)

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

        val index = FontScale.getFontScaleValue(activity).index

        for (i in 0 until childCount) {
            val v = linearLayout.getChildAt(i)

            if (v is CheckedTextView) {
                v.isChecked = i == index

                v.setOnClickListener {
                    dialog.dismiss()
                    FontScale.updateFontScale(activity, i)
                    vectorConfiguration.applyToApplicationContext()
                    activity.restart()
                }
            }
        }
    }

    private fun updateBubbleDependencies(bubbleStyle: String, bubbleTimeLocation: String) {
        bubbleTimeLocationPref?.setEnabled(BubbleThemeUtils.isBubbleTimeLocationSettingAllowed(bubbleStyle))
        alwaysShowTimestampsPref?.setEnabled(!BubbleThemeUtils.forceAlwaysShowTimestamps(bubbleStyle, bubbleTimeLocation))
    }
}
