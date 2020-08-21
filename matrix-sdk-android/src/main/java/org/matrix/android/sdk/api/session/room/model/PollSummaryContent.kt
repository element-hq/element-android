/*
 * Copyright 2020 New Vector Ltd
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
package org.matrix.android.sdk.api.session.room.model

import com.squareup.moshi.JsonClass

/**
 * Contains an aggregated summary info of the poll response.
 * Put pre-computed info that you want to access quickly without having
 * to go through all references events
 */
@JsonClass(generateAdapter = true)
data class PollSummaryContent(
        // Index of my vote
        var myVote: Int? = null,
        // Array of VoteInfo, list is constructed so that there is only one vote by user
        // And that optionIndex is valid
        var votes: List<VoteInfo>? = null
) {

    fun voteCount(): Int {
        return votes?.size ?: 0
    }

    fun voteCountForOption(optionIndex: Int) : Int {
        return votes?.filter { it.optionIndex == optionIndex }?.count() ?: 0
    }
}

@JsonClass(generateAdapter = true)
data class VoteInfo(
        val userId: String,
        val optionIndex: Int,
        val voteTimestamp: Long
)
