/*
 * Copyright 2019 New Vector Ltd
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

package org.matrix.android.sdk.api.session.room.model.call

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * This event is sent by callers after sending an invite and by the callee after answering.
 * Its purpose is to give the other party additional ICE candidates to try using to communicate.
 */
@JsonClass(generateAdapter = true)
data class CallCandidatesContent(
        /**
         * Required. The ID of the call this event relates to.
         */
        @Json(name = "call_id") val callId: String,
        /**
         * Required. Array of objects describing the candidates.
         */
        @Json(name = "candidates") val candidates: List<Candidate> = emptyList(),
        /**
         * Required. The version of the VoIP specification this messages adheres to. This specification is version 0.
         */
        @Json(name = "version") val version: Int = 0
) {

    @JsonClass(generateAdapter = true)
    data class Candidate(
            /**
             * Required. The SDP media type this candidate is intended for.
             */
            @Json(name = "sdpMid") val sdpMid: String,
            /**
             * Required. The index of the SDP 'm' line this candidate is intended for.
             */
            @Json(name = "sdpMLineIndex") val sdpMLineIndex: Int,
            /**
             * Required. The SDP 'a' line of the candidate.
             */
            @Json(name = "candidate") val candidate: String
    )
}
