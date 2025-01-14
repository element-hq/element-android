/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.call.transfer

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

sealed class CallTransferResult : Parcelable {
    @Parcelize data class ConnectWithUserId(val consultFirst: Boolean, val selectedUserId: String) : CallTransferResult()
    @Parcelize data class ConnectWithPhoneNumber(val consultFirst: Boolean, val phoneNumber: String) : CallTransferResult()
}
