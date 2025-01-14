/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.home.room.threads.list.viewmodel

import com.airbnb.mvrx.Async
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.Uninitialized
import im.vector.app.features.home.room.threads.arguments.ThreadListArgs
import org.matrix.android.sdk.api.session.threads.ThreadTimelineEvent

data class ThreadListViewState(
        val rootThreadEventList: Async<List<ThreadTimelineEvent>> = Uninitialized,
        val shouldFilterThreads: Boolean = false,
        val isLoading: Boolean = false,
        val roomId: String
) : MavericksState {
    constructor(args: ThreadListArgs) : this(roomId = args.roomId)
}
