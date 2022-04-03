/*
 * Copyright 2022 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.api.session.room.model.message

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = false)
enum class PollType {
    /**
     * Voters should see results as soon as they have voted.
     */
    @Json(name = "org.matrix.msc3381.poll.disclosed")
    DISCLOSED_UNSTABLE,

    @Json(name = "m.poll.disclosed")
    DISCLOSED,

    /**
     * Results should be only revealed when the poll is ended.
     */
    @Json(name = "org.matrix.msc3381.poll.undisclosed")
    UNDISCLOSED_UNSTABLE,

    @Json(name = "m.poll.undisclosed")
    UNDISCLOSED
}
