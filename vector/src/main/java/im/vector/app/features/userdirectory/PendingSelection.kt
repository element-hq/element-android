/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.userdirectory

import im.vector.app.features.displayname.getBestName
import org.matrix.android.sdk.api.session.identity.ThreePid
import org.matrix.android.sdk.api.session.user.model.User
import org.matrix.android.sdk.api.util.toMatrixItem

sealed class PendingSelection {
    data class UserPendingSelection(val user: User, var isUnknownUser: Boolean = false) : PendingSelection()
    data class ThreePidPendingSelection(val threePid: ThreePid) : PendingSelection()

    fun getBestName(): String {
        return when (this) {
            is UserPendingSelection -> user.toMatrixItem().getBestName()
            is ThreePidPendingSelection -> threePid.value
        }
    }

    fun getMxId(): String {
        return when (this) {
            is UserPendingSelection -> user.userId
            is ThreePidPendingSelection -> threePid.value
        }
    }
}
