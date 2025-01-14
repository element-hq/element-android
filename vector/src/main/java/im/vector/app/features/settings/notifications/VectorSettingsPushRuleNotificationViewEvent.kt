/*
 * Copyright 2023, 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.settings.notifications

import im.vector.app.core.platform.VectorViewEvents

sealed interface VectorSettingsPushRuleNotificationViewEvent : VectorViewEvents {
    /**
     * A global push rule checked state has changed.
     *
     * @property ruleId the global rule id which has been updated.
     * @property checked whether the global rule is checked.
     * @property failure whether there has been a failure when updating the global rule (ie. a sub rule has not been updated).
     */
    data class PushRuleUpdated(val ruleId: String, val checked: Boolean, val failure: Throwable? = null) : VectorSettingsPushRuleNotificationViewEvent

    /**
     * A failure has occurred.
     *
     * @property ruleId the global rule id related to the failure.
     * @property throwable the related exception, if any.
     */
    data class Failure(val ruleId: String, val throwable: Throwable?) : VectorSettingsPushRuleNotificationViewEvent
}
