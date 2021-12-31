/*
 * Copyright (c) 2020 New Vector Ltd
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

package im.vector.app.features.userdirectory

import im.vector.app.features.displayname.getBestName
import org.matrix.android.sdk.api.session.identity.ThreePid
import org.matrix.android.sdk.api.session.user.model.User
import org.matrix.android.sdk.api.util.toMatrixItem

sealed class PendingSelection {
    data class UserPendingSelection(val user: User) : PendingSelection()
    data class ThreePidPendingSelection(val threePid: ThreePid) : PendingSelection()

    fun getBestName(): String {
        return when (this) {
            is UserPendingSelection     -> user.toMatrixItem().getBestName()
            is ThreePidPendingSelection -> threePid.value
        }
    }

    fun getMxId(): String {
        return when (this) {
            is UserPendingSelection     -> user.userId
            is ThreePidPendingSelection -> threePid.value
        }
    }
}
