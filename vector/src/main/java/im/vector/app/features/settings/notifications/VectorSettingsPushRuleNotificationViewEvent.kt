/*
 * Copyright (c) 2023 New Vector Ltd
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
