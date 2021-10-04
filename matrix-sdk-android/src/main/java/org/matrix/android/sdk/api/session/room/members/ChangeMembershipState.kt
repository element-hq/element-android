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

package org.matrix.android.sdk.api.session.room.members

sealed class ChangeMembershipState {
    object Unknown : ChangeMembershipState()
    object Joining : ChangeMembershipState()
    data class FailedJoining(val throwable: Throwable) : ChangeMembershipState()
    object Joined : ChangeMembershipState()
    object Leaving : ChangeMembershipState()
    data class FailedLeaving(val throwable: Throwable) : ChangeMembershipState()
    object Left : ChangeMembershipState()

    fun isInProgress() = this is Joining || this is Leaving

    fun isSuccessful() = this is Joined || this is Left

    fun isFailed() = this is FailedJoining || this is FailedLeaving
}
