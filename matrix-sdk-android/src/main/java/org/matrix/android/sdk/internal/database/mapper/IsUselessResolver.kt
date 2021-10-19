/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.database.mapper

import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.toContent

internal object IsUselessResolver {

    /**
     * @return true if the event is useless
     */
    fun isUseless(event: Event): Boolean {
        return when (event.type) {
            EventType.STATE_ROOM_MEMBER -> {
                // Call toContent(), to filter out null value
                event.content != null &&
                        event.content.toContent() == event.resolvedPrevContent()?.toContent()
            }
            else                        -> false
        }
    }
}
