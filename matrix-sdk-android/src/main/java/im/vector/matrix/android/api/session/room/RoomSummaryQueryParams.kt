/*
 * Copyright 2020 New Vector Ltd
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

package im.vector.matrix.android.api.session.room

import im.vector.matrix.android.api.session.room.model.Membership

/**
 * This class can be used to filter room summaries to use with:
 * [im.vector.matrix.android.api.session.room.Room] and [im.vector.matrix.android.api.session.room.RoomService]
 */
data class RoomSummaryQueryParams(
        /**
         * Set to true if you want only non null display name. True by default
         */
        val filterDisplayName: Boolean = true,
        /**
         * Set to true if you want only non null canonical alias. False by default.
         */
        val filterCanonicalAlias: Boolean = false,
        /**
         * Set the list of memberships you want to filter on. By default, all memberships.
         */
        val memberships: List<Membership> = Membership.all()
)
