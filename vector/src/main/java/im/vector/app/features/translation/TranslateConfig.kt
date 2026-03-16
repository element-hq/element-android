/*
 * Copyright 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.translation

import android.content.SharedPreferences
import androidx.core.content.edit
import im.vector.app.core.di.DefaultPreferences
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TranslateConfig @Inject constructor(
        @DefaultPreferences private val prefs: SharedPreferences
) {
    companion object {
        private const val KEY_API_URL = "translate_api_url"
        private const val KEY_API_KEY = "translate_api_key"
        private const val KEY_MODEL = "translate_model"
        private const val KEY_TARGET_LANGUAGE = "translate_target_language"
        private const val KEY_ROOM_LANGUAGE = "translate_room_language"
        private const val KEY_ENABLED = "translate_enabled"
        private const val KEY_AUTO_TRANSLATE = "translate_auto_translate"
        private const val KEY_REFORMULATION_ENABLED = "translate_reformulation_enabled"
        private const val KEY_SUMMARY_ENABLED = "translate_summary_enabled"
        private const val KEY_SUGGESTED_REPLIES_ENABLED = "translate_suggested_replies_enabled"
        private const val KEY_NOTIFICATION_SUMMARY_ENABLED = "translate_notification_summary_enabled"
    }

    var apiUrl: String
        get() = prefs.getString(KEY_API_URL, "http://localhost:11434/v1") ?: "http://localhost:11434/v1"
        set(value) = prefs.edit { putString(KEY_API_URL, value) }

    var apiKey: String
        get() = prefs.getString(KEY_API_KEY, "") ?: ""
        set(value) = prefs.edit { putString(KEY_API_KEY, value) }

    var model: String
        get() = prefs.getString(KEY_MODEL, "llama3") ?: "llama3"
        set(value) = prefs.edit { putString(KEY_MODEL, value) }

    var targetLanguage: String
        get() = prefs.getString(KEY_TARGET_LANGUAGE, "French") ?: "French"
        set(value) = prefs.edit { putString(KEY_TARGET_LANGUAGE, value) }

    var roomLanguage: String
        get() = prefs.getString(KEY_ROOM_LANGUAGE, "") ?: ""
        set(value) = prefs.edit { putString(KEY_ROOM_LANGUAGE, value) }

    var enabled: Boolean
        get() = prefs.getBoolean(KEY_ENABLED, false)
        set(value) = prefs.edit { putBoolean(KEY_ENABLED, value) }

    var autoTranslate: Boolean
        get() = prefs.getBoolean(KEY_AUTO_TRANSLATE, false)
        set(value) = prefs.edit { putBoolean(KEY_AUTO_TRANSLATE, value) }

    var reformulationEnabled: Boolean
        get() = prefs.getBoolean(KEY_REFORMULATION_ENABLED, true)
        set(value) = prefs.edit { putBoolean(KEY_REFORMULATION_ENABLED, value) }

    var summaryEnabled: Boolean
        get() = prefs.getBoolean(KEY_SUMMARY_ENABLED, true)
        set(value) = prefs.edit { putBoolean(KEY_SUMMARY_ENABLED, value) }

    var suggestedRepliesEnabled: Boolean
        get() = prefs.getBoolean(KEY_SUGGESTED_REPLIES_ENABLED, true)
        set(value) = prefs.edit { putBoolean(KEY_SUGGESTED_REPLIES_ENABLED, value) }

    var notificationSummaryEnabled: Boolean
        get() = prefs.getBoolean(KEY_NOTIFICATION_SUMMARY_ENABLED, true)
        set(value) = prefs.edit { putBoolean(KEY_NOTIFICATION_SUMMARY_ENABLED, value) }
}
