/*
 * Copyright 2023, 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.settings.notifications.defaults

import android.os.Bundle
import im.vector.app.R
import im.vector.app.core.preference.VectorPreferenceCategory
import im.vector.app.features.analytics.plan.MobileScreen
import im.vector.app.features.settings.notifications.VectorSettingsPushRuleNotificationFragment
import im.vector.lib.strings.CommonStrings
import org.matrix.android.sdk.api.session.pushrules.RuleIds

class VectorSettingsDefaultNotificationFragment :
        VectorSettingsPushRuleNotificationFragment() {

    override var titleRes: Int = CommonStrings.settings_notification_default

    override val preferenceXmlRes = R.xml.vector_settings_notification_default

    override val prefKeyToPushRuleId = mapOf(
            "SETTINGS_PUSH_RULE_MESSAGES_IN_ONE_TO_ONE_PREFERENCE_KEY" to RuleIds.RULE_ID_ONE_TO_ONE_ROOM,
            "SETTINGS_PUSH_RULE_MESSAGES_IN_GROUP_CHAT_PREFERENCE_KEY" to RuleIds.RULE_ID_ALL_OTHER_MESSAGES_ROOMS,
            "SETTINGS_PUSH_RULE_MESSAGES_IN_E2E_ONE_ONE_CHAT_PREFERENCE_KEY" to RuleIds.RULE_ID_ONE_TO_ONE_ENCRYPTED_ROOM,
            "SETTINGS_PUSH_RULE_MESSAGES_IN_E2E_GROUP_CHAT_PREFERENCE_KEY" to RuleIds.RULE_ID_ENCRYPTED
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        analyticsScreenName = MobileScreen.ScreenName.SettingsDefaultNotifications
    }

    override fun bindPref() {
        super.bindPref()
        val category = findPreference<VectorPreferenceCategory>("SETTINGS_DEFAULT")!!
        category.isIconSpaceReserved = false
    }
}
