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

package im.vector.app.features.home.room.detail.arguments

import android.os.Parcelable
import im.vector.app.features.home.room.threads.arguments.ThreadTimelineArgs
import im.vector.app.features.share.SharedData
import kotlinx.parcelize.Parcelize

@Parcelize
data class TimelineArgs(
        val roomId: String,
        val eventId: String? = null,
        val sharedData: SharedData? = null,
        val openShareSpaceForId: String? = null,
        val threadTimelineArgs: ThreadTimelineArgs? = null,
        val switchToParentSpace: Boolean = false,
        val isInviteAlreadyAccepted: Boolean = false
) : Parcelable
