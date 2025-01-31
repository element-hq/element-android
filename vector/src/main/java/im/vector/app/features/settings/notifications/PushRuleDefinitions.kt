/*
 * Copyright 2021-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.settings.notifications

import org.matrix.android.sdk.api.session.pushrules.RuleIds

fun getStandardAction(ruleId: String, index: NotificationIndex): StandardActions? {
    return when (ruleId) {
        RuleIds.RULE_ID_CONTAIN_DISPLAY_NAME ->
            when (index) {
                NotificationIndex.OFF -> StandardActions.Disabled
                NotificationIndex.SILENT -> StandardActions.Notify
                NotificationIndex.NOISY -> StandardActions.HighlightDefaultSound
            }
        RuleIds.RULE_ID_CONTAIN_USER_NAME ->
            when (index) {
                NotificationIndex.OFF -> StandardActions.Disabled
                NotificationIndex.SILENT -> StandardActions.Notify
                NotificationIndex.NOISY -> StandardActions.HighlightDefaultSound
            }
        RuleIds.RULE_ID_ROOM_NOTIF ->
            when (index) {
                NotificationIndex.OFF -> StandardActions.Disabled
                NotificationIndex.SILENT -> StandardActions.Notify
                NotificationIndex.NOISY -> StandardActions.Highlight
            }
        RuleIds.RULE_ID_ONE_TO_ONE_ROOM ->
            when (index) {
                NotificationIndex.OFF -> StandardActions.DontNotify
                NotificationIndex.SILENT -> StandardActions.Notify
                NotificationIndex.NOISY -> StandardActions.NotifyDefaultSound
            }
        RuleIds.RULE_ID_ONE_TO_ONE_ENCRYPTED_ROOM ->
            when (index) {
                NotificationIndex.OFF -> StandardActions.DontNotify
                NotificationIndex.SILENT -> StandardActions.Notify
                NotificationIndex.NOISY -> StandardActions.NotifyDefaultSound
            }
        RuleIds.RULE_ID_ALL_OTHER_MESSAGES_ROOMS ->
            when (index) {
                NotificationIndex.OFF -> StandardActions.DontNotify
                NotificationIndex.SILENT -> StandardActions.Notify
                NotificationIndex.NOISY -> StandardActions.NotifyDefaultSound
            }
        RuleIds.RULE_ID_ENCRYPTED ->
            when (index) {
                NotificationIndex.OFF -> StandardActions.DontNotify
                NotificationIndex.SILENT -> StandardActions.Notify
                NotificationIndex.NOISY -> StandardActions.NotifyDefaultSound
            }
        RuleIds.RULE_ID_INVITE_ME ->
            when (index) {
                NotificationIndex.OFF -> StandardActions.Disabled
                NotificationIndex.SILENT -> StandardActions.Notify
                NotificationIndex.NOISY -> StandardActions.NotifyDefaultSound
            }
        RuleIds.RULE_ID_CALL ->
            when (index) {
                NotificationIndex.OFF -> StandardActions.Disabled
                NotificationIndex.SILENT -> StandardActions.Notify
                NotificationIndex.NOISY -> StandardActions.NotifyRingSound
            }
        RuleIds.RULE_ID_SUPPRESS_BOTS_NOTIFICATIONS ->
            when (index) {
                NotificationIndex.OFF -> StandardActions.DontNotify
                NotificationIndex.SILENT -> StandardActions.Disabled
                NotificationIndex.NOISY -> StandardActions.NotifyDefaultSound
            }
        RuleIds.RULE_ID_TOMBSTONE ->
            when (index) {
                NotificationIndex.OFF -> StandardActions.Disabled
                NotificationIndex.SILENT -> StandardActions.Notify
                NotificationIndex.NOISY -> StandardActions.Highlight
            }
        RuleIds.RULE_ID_KEYWORDS ->
            when (index) {
                NotificationIndex.OFF -> StandardActions.Disabled
                NotificationIndex.SILENT -> StandardActions.Notify
                NotificationIndex.NOISY -> StandardActions.HighlightDefaultSound
            }
        else -> null
    }
}
