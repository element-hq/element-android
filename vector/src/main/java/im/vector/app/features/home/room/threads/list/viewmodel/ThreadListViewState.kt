/*
 * Copyright 2021 New Vector Ltd
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

package im.vector.app.features.home.room.threads.list.viewmodel

import com.airbnb.mvrx.Async
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.Uninitialized
import im.vector.app.features.home.room.threads.arguments.ThreadListArgs
import org.matrix.android.sdk.api.session.room.threads.model.ThreadSummary
import org.matrix.android.sdk.api.session.threads.ThreadTimelineEvent

data class ThreadListViewState(
        val threadSummaryList: Async<List<ThreadSummary>> = Uninitialized,
        val rootThreadEventList: Async<List<ThreadTimelineEvent>> = Uninitialized,
        val shouldFilterThreads: Boolean = false,
        val isLoading: Boolean = false,
        val roomId: String
) : MavericksState {
    constructor(args: ThreadListArgs) : this(roomId = args.roomId)
}
