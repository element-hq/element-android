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

package org.matrix.android.sdk.api.query

/**
 * To filter by Room category.
 * @see [org.matrix.android.sdk.api.session.room.RoomSummaryQueryParams]
 */
enum class RoomCategoryFilter {
    /**
     * Get only the DM, i.e. the rooms referenced in `m.direct` account data.
     */
    ONLY_DM,

    /**
     * Get only the Room, not the DM, i.e. the rooms not referenced in `m.direct` account data.
     */
    ONLY_ROOMS,

    /**
     * Get the room with non-0 notifications.
     */
    ONLY_WITH_NOTIFICATIONS,
}
