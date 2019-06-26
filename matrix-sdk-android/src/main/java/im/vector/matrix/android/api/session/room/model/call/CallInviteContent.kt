/*
 *
 *  * Copyright 2019 New Vector Ltd
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package im.vector.matrix.android.api.session.room.model.call

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CallInviteContent(
        @Json(name = "call_id") val callId: String,
        @Json(name = "version") val version: Int,
        @Json(name = "lifetime") val lifetime: Int,
        @Json(name = "offer") val offer: Offer
) {

    @JsonClass(generateAdapter = true)
    data class Offer(
            @Json(name = "type") val type: String,
            @Json(name = "sdp") val sdp: String
    ) {
        companion object {
            const val SDP_VIDEO = "m=video"
        }
    }


    fun isVideo(): Boolean = offer.sdp.contains(Offer.SDP_VIDEO)
}