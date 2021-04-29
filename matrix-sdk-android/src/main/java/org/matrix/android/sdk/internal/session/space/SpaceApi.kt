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

package org.matrix.android.sdk.internal.session.space

import org.matrix.android.sdk.internal.network.NetworkConstants
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path

internal interface SpaceApi {

    /**
     *
     * POST /_matrix/client/r0/rooms/{roomID}/spaces
     *  {
     *    "max_rooms_per_space": 5,      // The maximum number of rooms/subspaces to return for a given space, if negative unbounded. default: -1.
     *    "auto_join_only": true,        // If true, only return m.space.child events with auto_join:true, default: false, which returns all events.
     *    "limit": 100,                  // The maximum number of rooms/subspaces to return, server can override this, default: 100.
     *    "batch": "opaque_string"       // A token to use if this is a subsequent HTTP hit, default: "".
     *  }
     *
     * Ref:
     * - MSC 2946 https://github.com/matrix-org/matrix-doc/blob/kegan/spaces-summary/proposals/2946-spaces-summary.md
     * - https://hackmd.io/fNYh4tjUT5mQfR1uuRzWDA
     */
    @POST(NetworkConstants.URI_API_PREFIX_PATH_UNSTABLE + "org.matrix.msc2946/rooms/{roomId}/spaces")
    suspend fun getSpaces(@Path("roomId") spaceId: String,
                  @Body params: SpaceSummaryParams): SpacesResponse
}
