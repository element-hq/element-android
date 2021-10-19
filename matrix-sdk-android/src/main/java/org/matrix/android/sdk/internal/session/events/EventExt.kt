/*
 * Copyright (c) 2021 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.session.events

import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.room.model.RoomMemberContent

internal fun Event.getFixedRoomMemberContent(): RoomMemberContent? {
    val content = content.toModel<RoomMemberContent>()
    // if user is leaving, we should grab his last name and avatar from prevContent
    return if (content?.membership?.isLeft() == true) {
        val prevContent = resolvedPrevContent().toModel<RoomMemberContent>()
        content.copy(
                displayName = prevContent?.displayName,
                avatarUrl = prevContent?.avatarUrl
        )
    } else {
        content
    }
}
