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

import org.matrix.android.sdk.api.pushrules.RuleIds

fun getStandardAction(ruleId: String, index: NotificationIndex): StandardActions? {
    return when (ruleId) {
        RuleIds.RULE_ID_CONTAIN_DISPLAY_NAME        ->
            when (index) {
                NotificationIndex.OFF    -> StandardActions.Disabled
                NotificationIndex.SILENT -> StandardActions.Notify
                NotificationIndex.NOISY  -> StandardActions.HighlightDefaultSound
            }
        RuleIds.RULE_ID_CONTAIN_USER_NAME           ->
            when (index) {
                NotificationIndex.OFF    -> StandardActions.Disabled
                NotificationIndex.SILENT -> StandardActions.Notify
                NotificationIndex.NOISY  -> StandardActions.HighlightDefaultSound
            }
        RuleIds.RULE_ID_ROOM_NOTIF                  ->
            when (index) {
                NotificationIndex.OFF    -> StandardActions.Disabled
                NotificationIndex.SILENT -> StandardActions.Notify
                NotificationIndex.NOISY  -> StandardActions.Highlight
            }
        RuleIds.RULE_ID_ONE_TO_ONE_ROOM             ->
            when (index) {
                NotificationIndex.OFF    -> StandardActions.DontNotify
                NotificationIndex.SILENT -> StandardActions.Notify
                NotificationIndex.NOISY  -> StandardActions.NotifyDefaultSound
            }
        RuleIds.RULE_ID_ONE_TO_ONE_ENCRYPTED_ROOM   ->
            when (index) {
                NotificationIndex.OFF    -> StandardActions.DontNotify
                NotificationIndex.SILENT -> StandardActions.Notify
                NotificationIndex.NOISY  -> StandardActions.NotifyDefaultSound
            }
        RuleIds.RULE_ID_ALL_OTHER_MESSAGES_ROOMS    ->
            when (index) {
                NotificationIndex.OFF    -> StandardActions.DontNotify
                NotificationIndex.SILENT -> StandardActions.Notify
                NotificationIndex.NOISY  -> StandardActions.NotifyDefaultSound
            }
        RuleIds.RULE_ID_ENCRYPTED                   ->
            when (index) {
                NotificationIndex.OFF    -> StandardActions.DontNotify
                NotificationIndex.SILENT -> StandardActions.Notify
                NotificationIndex.NOISY  -> StandardActions.NotifyDefaultSound
            }
        RuleIds.RULE_ID_INVITE_ME                   ->
            when (index) {
                NotificationIndex.OFF    -> StandardActions.Disabled
                NotificationIndex.SILENT -> StandardActions.Notify
                NotificationIndex.NOISY  -> StandardActions.NotifyDefaultSound
            }
        RuleIds.RULE_ID_CALL                        ->
            when (index) {
                NotificationIndex.OFF    -> StandardActions.Disabled
                NotificationIndex.SILENT -> StandardActions.Notify
                NotificationIndex.NOISY  -> StandardActions.NotifyRingSound
            }
        RuleIds.RULE_ID_SUPPRESS_BOTS_NOTIFICATIONS ->
            when (index) {
                NotificationIndex.OFF    -> StandardActions.DontNotify
                NotificationIndex.SILENT -> StandardActions.Disabled
                NotificationIndex.NOISY  -> StandardActions.NotifyDefaultSound
            }
        RuleIds.RULE_ID_TOMBSTONE                   ->
            when (index) {
                NotificationIndex.OFF    -> StandardActions.Disabled
                NotificationIndex.SILENT -> StandardActions.Notify
                NotificationIndex.NOISY  -> StandardActions.Highlight
            }
        RuleIds.RULE_ID_KEYWORDS                    ->
            when (index) {
                NotificationIndex.OFF    -> StandardActions.Disabled
                NotificationIndex.SILENT -> StandardActions.Notify
                NotificationIndex.NOISY  -> StandardActions.HighlightDefaultSound
            }
        else                                        -> null
    }
}
