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

package org.matrix.android.sdk.api.session.room.reporting

/**
 * This interface defines methods to report content of an event.
 */
interface ReportingService {

    /**
     * Report content
     * Ref: https://matrix.org/docs/spec/client_server/latest#post-matrix-client-r0-rooms-roomid-report-eventid
     */
    suspend fun reportContent(eventId: String, score: Int, reason: String)

    /**
     * Report a room.
     */
    suspend fun reportRoom(reason: String)
}
