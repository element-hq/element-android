package im.vector.matrix.android.api.pushrules

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
import im.vector.matrix.android.api.session.events.model.Event
import im.vector.matrix.android.api.session.room.model.PowerLevels


class SenderNotificationPermissionCondition(val key: String) : Condition(Kind.sender_notification_permission) {

    override fun isSatisfied(conditionResolver: ConditionResolver): Boolean {
        return conditionResolver.resolveSenderNotificationPermissionCondition(this)
    }

    override fun technicalDescription(): String {
        return "User power level <$key>"
    }


    fun isSatisfied(event: Event, powerLevels: PowerLevels): Boolean {
        return event.senderId != null && powerLevels.getUserPowerLevel(event.senderId) >= powerLevels.notificationLevel(key)
    }
}