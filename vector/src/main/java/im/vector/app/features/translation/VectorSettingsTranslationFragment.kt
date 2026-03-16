/*
 * Copyright 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.translation

import android.os.Bundle
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.SwitchPreference
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.R
import im.vector.app.features.settings.VectorSettingsBaseFragment
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class VectorSettingsTranslationFragment : VectorSettingsBaseFragment() {

    override var titleRes: Int = R.string.settings_translation
    override val preferenceXmlRes = R.xml.vector_settings_translation

    @Inject lateinit var translateConfig: TranslateConfig
    @Inject lateinit var translationService: TranslationService

    private val enabledPref by lazy { findPreference<SwitchPreference>("translate_enabled")!! }
    private val autoTranslatePref by lazy { findPreference<SwitchPreference>("translate_auto_translate")!! }
    private val apiUrlPref by lazy { findPreference<EditTextPreference>("translate_api_url")!! }
    private val apiKeyPref by lazy { findPreference<EditTextPreference>("translate_api_key")!! }
    private val modelPref by lazy { findPreference<EditTextPreference>("translate_model")!! }
    private val targetLanguagePref by lazy { findPreference<EditTextPreference>("translate_target_language")!! }
    private val roomLanguagePref by lazy { findPreference<EditTextPreference>("translate_room_language")!! }
    private val testConnectionPref by lazy { findPreference<Preference>("translate_test_connection")!! }
    private val cacheInfoPref by lazy { findPreference<Preference>("translate_cache_info")!! }
    private val clearCachePref by lazy { findPreference<Preference>("translate_clear_cache")!! }
    private val reformulationPref by lazy { findPreference<SwitchPreference>("translate_reformulation_enabled")!! }
    private val summaryPref by lazy { findPreference<SwitchPreference>("translate_summary_enabled")!! }
    private val suggestedRepliesPref by lazy { findPreference<SwitchPreference>("translate_suggested_replies_enabled")!! }
    private val notificationSummaryPref by lazy { findPreference<SwitchPreference>("translate_notification_summary_enabled")!! }

    override fun bindPref() {
        // Initialize values
        enabledPref.isChecked = translateConfig.enabled
        autoTranslatePref.isChecked = translateConfig.autoTranslate
        apiUrlPref.text = translateConfig.apiUrl
        apiUrlPref.summary = translateConfig.apiUrl
        apiKeyPref.text = translateConfig.apiKey
        apiKeyPref.summary = if (translateConfig.apiKey.isBlank()) "(empty)" else "****"
        modelPref.text = translateConfig.model
        modelPref.summary = translateConfig.model
        targetLanguagePref.text = translateConfig.targetLanguage
        targetLanguagePref.summary = translateConfig.targetLanguage
        roomLanguagePref.text = translateConfig.roomLanguage
        roomLanguagePref.summary = translateConfig.roomLanguage.ifBlank { "(disabled)" }
        updateCacheInfo()

        // Listeners
        enabledPref.setOnPreferenceChangeListener { _, newValue ->
            translateConfig.enabled = newValue as Boolean
            true
        }

        autoTranslatePref.setOnPreferenceChangeListener { _, newValue ->
            translateConfig.autoTranslate = newValue as Boolean
            true
        }

        apiUrlPref.setOnPreferenceChangeListener { _, newValue ->
            val url = newValue as String
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                Toast.makeText(requireContext(), "URL must start with http:// or https://", Toast.LENGTH_SHORT).show()
                return@setOnPreferenceChangeListener false
            }
            val isLocalhost = url.contains("localhost") || url.contains("127.0.0.1") || url.contains("0.0.0.0")
            if (!isLocalhost && !url.startsWith("https://")) {
                Toast.makeText(requireContext(), "Warning: Non-localhost URLs should use HTTPS. Message content will be sent to this server.", Toast.LENGTH_LONG).show()
            }
            translateConfig.apiUrl = url
            apiUrlPref.summary = url
            true
        }

        apiKeyPref.setOnPreferenceChangeListener { _, newValue ->
            val key = newValue as String
            translateConfig.apiKey = key
            apiKeyPref.summary = if (key.isBlank()) "(empty)" else "****"
            true
        }

        modelPref.setOnPreferenceChangeListener { _, newValue ->
            val model = newValue as String
            translateConfig.model = model
            modelPref.summary = model
            true
        }

        targetLanguagePref.setOnPreferenceChangeListener { _, newValue ->
            val lang = newValue as String
            translateConfig.targetLanguage = lang
            targetLanguagePref.summary = lang
            true
        }

        roomLanguagePref.setOnPreferenceChangeListener { _, newValue ->
            val lang = newValue as String
            translateConfig.roomLanguage = lang
            roomLanguagePref.summary = lang.ifBlank { "(disabled)" }
            true
        }

        testConnectionPref.setOnPreferenceClickListener {
            testConnection()
            true
        }

        clearCachePref.setOnPreferenceClickListener {
            translationService.clearCache()
            updateCacheInfo()
            Toast.makeText(requireContext(), R.string.translation_cache_cleared, Toast.LENGTH_SHORT).show()
            true
        }

        // AI Feature toggles
        reformulationPref.isChecked = translateConfig.reformulationEnabled
        reformulationPref.setOnPreferenceChangeListener { _, newValue ->
            translateConfig.reformulationEnabled = newValue as Boolean
            true
        }

        summaryPref.isChecked = translateConfig.summaryEnabled
        summaryPref.setOnPreferenceChangeListener { _, newValue ->
            translateConfig.summaryEnabled = newValue as Boolean
            true
        }

        suggestedRepliesPref.isChecked = translateConfig.suggestedRepliesEnabled
        suggestedRepliesPref.setOnPreferenceChangeListener { _, newValue ->
            translateConfig.suggestedRepliesEnabled = newValue as Boolean
            true
        }

        notificationSummaryPref.isChecked = translateConfig.notificationSummaryEnabled
        notificationSummaryPref.setOnPreferenceChangeListener { _, newValue ->
            translateConfig.notificationSummaryEnabled = newValue as Boolean
            true
        }
    }

    private fun updateCacheInfo() {
        cacheInfoPref.summary = getString(R.string.translation_cache_count, translationService.getCacheSize())
    }

    private fun testConnection() {
        testConnectionPref.summary = getString(R.string.translation_testing)
        lifecycleScope.launch {
            val result = translationService.testConnection()
            result.fold(
                    onSuccess = { models ->
                        testConnectionPref.summary = getString(R.string.translation_test_success, models.joinToString(", "))
                    },
                    onFailure = { error ->
                        testConnectionPref.summary = getString(R.string.translation_test_failed, error.message ?: "Unknown error")
                    }
            )
        }
    }
}
