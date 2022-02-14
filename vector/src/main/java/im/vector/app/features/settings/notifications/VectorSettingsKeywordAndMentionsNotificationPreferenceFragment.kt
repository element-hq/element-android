/*
 * Copyright (c) 2021 New Vector Ltd
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

package im.vector.app.features.settings.notifications

import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import im.vector.app.R
import im.vector.app.core.preference.KeywordPreference
import im.vector.app.core.preference.VectorCheckboxPreference
import im.vector.app.core.preference.VectorPreference
import im.vector.app.core.preference.VectorPreferenceCategory
import im.vector.app.features.analytics.plan.MobileScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.matrix.android.sdk.api.pushrules.RuleIds
import org.matrix.android.sdk.api.pushrules.RuleKind
import org.matrix.android.sdk.api.pushrules.rest.PushRule
import org.matrix.android.sdk.api.pushrules.toJson

class VectorSettingsKeywordAndMentionsNotificationPreferenceFragment :
        VectorSettingsPushRuleNotificationPreferenceFragment() {

    override var titleRes: Int = R.string.settings_notification_mentions_and_keywords

    override val preferenceXmlRes = R.xml.vector_settings_notification_mentions_and_keywords

    private var keywordsHasFocus = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        analyticsScreenName = MobileScreen.ScreenName.SettingsMentionsAndKeywords
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        session.getKeywords().observe(viewLifecycleOwner, this::updateWithKeywords)
    }

    override fun bindPref() {
        super.bindPref()
        val mentionCategory = findPreference<VectorPreferenceCategory>("SETTINGS_KEYWORDS_AND_MENTIONS")!!
        mentionCategory.isIconSpaceReserved = false

        val yourKeywordsCategory = findPreference<VectorPreferenceCategory>("SETTINGS_YOUR_KEYWORDS")!!
        yourKeywordsCategory.isIconSpaceReserved = false

        val keywordRules = session.getPushRules().content?.filter { !it.ruleId.startsWith(".") }.orEmpty()
        val enableKeywords = keywordRules.isEmpty() || keywordRules.any(PushRule::enabled)

        val editKeywordPreference = findPreference<KeywordPreference>("SETTINGS_KEYWORD_EDIT")!!
        editKeywordPreference.isEnabled = enableKeywords

        val keywordPreference = findPreference<VectorCheckboxPreference>("SETTINGS_PUSH_RULE_MESSAGES_CONTAINING_KEYWORDS_PREFERENCE_KEY")!!
        keywordPreference.isIconSpaceReserved = false
        keywordPreference.isChecked = enableKeywords

        val footerPreference = findPreference<VectorPreference>("SETTINGS_KEYWORDS_FOOTER")!!
        footerPreference.isIconSpaceReserved = false
        keywordPreference.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
            val keywords = editKeywordPreference.keywords
            val newChecked = newValue as Boolean
            displayLoadingView()
            updateKeywordPushRules(keywords, newChecked) { result ->
                hideLoadingView()
                if (!isAdded) {
                    return@updateKeywordPushRules
                }
                result.onSuccess {
                    keywordPreference.isChecked = newChecked
                    editKeywordPreference.isEnabled = newChecked
                }
                result.onFailure { failure ->
                    refreshDisplay()
                    displayErrorDialog(failure)
                }
            }
            false
        }

        editKeywordPreference.listener = object : KeywordPreference.Listener {
            override fun onFocusDidChange(hasFocus: Boolean) {
                keywordsHasFocus = true
            }

            override fun didAddKeyword(keyword: String) {
                addKeyword(keyword)
            }

            override fun didRemoveKeyword(keyword: String) {
                removeKeyword(keyword)
            }
        }
    }

    fun updateKeywordPushRules(keywords: Set<String>, checked: Boolean, completion: (Result<Unit>) -> Unit) {
        val newIndex = if (checked) NotificationIndex.NOISY else NotificationIndex.OFF
        val standardAction = getStandardAction(RuleIds.RULE_ID_KEYWORDS, newIndex) ?: return
        val enabled = standardAction != StandardActions.Disabled
        val newActions = standardAction.actions

        lifecycleScope.launch {
            val results = keywords.map {
                runCatching {
                    withContext(Dispatchers.Default) {
                        session.updatePushRuleActions(RuleKind.CONTENT,
                                it,
                                enabled,
                                newActions)
                    }
                }
            }
            val firstError = results.firstNotNullOfOrNull(Result<Unit>::exceptionOrNull)
            if (firstError == null) {
                completion(Result.success(Unit))
            } else {
                completion(Result.failure(firstError))
            }
        }
    }

    fun updateWithKeywords(keywords: Set<String>) {
        val editKeywordPreference = findPreference<KeywordPreference>("SETTINGS_KEYWORD_EDIT") ?: return
        editKeywordPreference.keywords = keywords
        if (keywordsHasFocus) {
            scrollToPreference(editKeywordPreference)
        }
    }

    fun addKeyword(keyword: String) {
        val standardAction = getStandardAction(RuleIds.RULE_ID_KEYWORDS, NotificationIndex.NOISY) ?: return
        val enabled = standardAction != StandardActions.Disabled
        val newActions = standardAction.actions ?: return
        val newRule = PushRule(actions = newActions.toJson(), pattern = keyword, enabled = enabled, ruleId = keyword)
        displayLoadingView()
        lifecycleScope.launch {
            val result = runCatching {
                session.addPushRule(RuleKind.CONTENT, newRule)
            }
            hideLoadingView()
            if (!isAdded) {
                return@launch
            }
            // Already added to UI, no-op on success

            result.onFailure(::displayErrorDialog)
        }
    }

    fun removeKeyword(keyword: String) {
        displayLoadingView()
        lifecycleScope.launch {
            val result = runCatching {
                session.removePushRule(RuleKind.CONTENT, keyword)
            }
            hideLoadingView()
            if (!isAdded) {
                return@launch
            }
            // Already added to UI, no-op on success

            result.onFailure(::displayErrorDialog)
        }
    }

    override val prefKeyToPushRuleId = mapOf(
            "SETTINGS_PUSH_RULE_CONTAINING_MY_DISPLAY_NAME_PREFERENCE_KEY" to RuleIds.RULE_ID_CONTAIN_DISPLAY_NAME,
            "SETTINGS_PUSH_RULE_CONTAINING_MY_USER_NAME_PREFERENCE_KEY" to RuleIds.RULE_ID_CONTAIN_USER_NAME,
            "SETTINGS_PUSH_RULE_MESSAGES_CONTAINING_AT_ROOM_PREFERENCE_KEY" to RuleIds.RULE_ID_ROOM_NOTIF
    )
}
