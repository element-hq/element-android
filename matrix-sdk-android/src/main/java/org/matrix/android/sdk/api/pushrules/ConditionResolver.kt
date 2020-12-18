/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.matrix.android.sdk.api.pushrules

import org.matrix.android.sdk.api.session.events.model.Event

/**
 * Acts like a visitor on Conditions.
 * This class as all required context needed to evaluate rules
 */
interface ConditionResolver {
    fun resolveEventMatchCondition(event: Event,
                                   condition: EventMatchCondition): Boolean

    fun resolveRoomMemberCountCondition(event: Event,
                                        condition: RoomMemberCountCondition): Boolean

    fun resolveSenderNotificationPermissionCondition(event: Event,
                                                     condition: SenderNotificationPermissionCondition): Boolean

    fun resolveContainsDisplayNameCondition(event: Event,
                                            condition: ContainsDisplayNameCondition): Boolean
}
