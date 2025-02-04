/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.home.room.threads.arguments

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.matrix.android.sdk.api.session.crypto.model.RoomEncryptionTrustLevel

@Parcelize
data class ThreadListArgs(
        val roomId: String,
        val displayName: String?,
        val avatarUrl: String?,
        val roomEncryptionTrustLevel: RoomEncryptionTrustLevel?
) : Parcelable
