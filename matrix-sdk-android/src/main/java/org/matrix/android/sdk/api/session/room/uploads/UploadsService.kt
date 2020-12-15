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

package org.matrix.android.sdk.api.session.room.uploads

/**
 * This interface defines methods to get event with uploads (= attachments) sent to a room. It's implemented at the room level.
 */
interface UploadsService {

    /**
     * Get a list of events containing URL sent to a room, from most recent to oldest one
     * @param numberOfEvents the expected number of events to retrieve. The result can contain less events.
     * @param since token to get next page, or null to get the first page
     */
    suspend fun getUploads(numberOfEvents: Int, since: String?): GetUploadsResult
}
