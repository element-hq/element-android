/*
 * Copyright (c) 2022 New Vector Ltd
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

package im.vector.app.features.analytics.extensions

import im.vector.app.features.analytics.plan.ViewRoom
import org.matrix.android.sdk.api.extensions.orFalse
import org.matrix.android.sdk.api.session.room.model.RoomSummary
import org.matrix.android.sdk.api.session.room.model.RoomType

fun RoomSummary?.toAnalyticsViewRoom(trigger: ViewRoom.Trigger?, selectedSpace: RoomSummary? = null, viaKeyboard: Boolean? = null): ViewRoom {
    val activeSpace = selectedSpace?.toActiveSpace() ?: ViewRoom.ActiveSpace.Home

    return ViewRoom(
            isDM = this?.isDirect.orFalse(),
            isSpace = this?.roomType == RoomType.SPACE,
            trigger = trigger,
            activeSpace = activeSpace,
            viaKeyboard = viaKeyboard
    )
}

private fun RoomSummary.toActiveSpace(): ViewRoom.ActiveSpace {
    return if (isPublic) ViewRoom.ActiveSpace.Public else ViewRoom.ActiveSpace.Private
}
