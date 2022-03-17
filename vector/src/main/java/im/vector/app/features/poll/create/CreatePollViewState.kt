/*
 * Copyright (c) 2021 New Vector Ltd
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

package im.vector.app.features.poll.create

import com.airbnb.mvrx.MavericksState
import im.vector.app.features.poll.PollMode
import org.matrix.android.sdk.api.session.room.model.message.PollType

data class CreatePollViewState(
    val roomId: String,
    val editedEventId: String?,
    val mode: PollMode,
    val question: String = "",
    val options: List<String> = List(CreatePollViewModel.MIN_OPTIONS_COUNT) { "" },
    val canCreatePoll: Boolean = false,
    val canAddMoreOptions: Boolean = true,
    val pollType: PollType = PollType.DISCLOSED_UNSTABLE
) : MavericksState {

    constructor(args: CreatePollArgs) : this(
            roomId = args.roomId,
            editedEventId = args.editedEventId,
            mode = args.mode
    )
}
