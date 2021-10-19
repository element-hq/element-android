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

import org.matrix.android.sdk.api.pushrules.rest.PushRule
import org.matrix.android.sdk.api.pushrules.toJson

enum class NotificationIndex {
    OFF,
    SILENT,
    NOISY;
}

/**
 * Given a push rule determine the NotificationIndex by comparing it to the static push rule definitions.
 * Used when determining the selected state of the PushRulePreference.
 */
val PushRule.notificationIndex: NotificationIndex? get() =
    NotificationIndex.values().firstOrNull {
        // Get the actions for the index
        val standardAction = getStandardAction(this.ruleId, it) ?: return@firstOrNull false
        val indexActions = standardAction.actions ?: listOf()
        // Check if the input rule matches a rule generated from the static rule definitions
        val targetRule = this.copy(enabled = standardAction != StandardActions.Disabled, actions = indexActions.toJson())
        ruleMatches(this, targetRule)
    }

/**
 * A check to determine if two push rules should be considered a match.
 */
private fun ruleMatches(rule: PushRule, targetRule: PushRule): Boolean {
    // Rules match if both are disabled, or if both are enabled and their highlight/sound/notify actions match up.
    return (!rule.enabled && !targetRule.enabled) ||
            (rule.enabled &&
            targetRule.enabled &&
            rule.getHighlight() == targetRule.getHighlight() &&
            rule.getNotificationSound() == targetRule.getNotificationSound() &&
            rule.shouldNotify() == targetRule.shouldNotify() &&
            rule.shouldNotNotify() == targetRule.shouldNotNotify())
}
