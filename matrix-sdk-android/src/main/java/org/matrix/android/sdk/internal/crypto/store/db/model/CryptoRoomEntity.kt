/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.crypto.store.db.model

import io.realm.RealmObject
import io.realm.annotations.PrimaryKey

internal open class CryptoRoomEntity(
        @PrimaryKey var roomId: String? = null,
        var algorithm: String? = null,
        var shouldEncryptForInvitedMembers: Boolean? = null,
        var blacklistUnverifiedDevices: Boolean = false,
        // Determines whether or not room history should be shared on new member invites
        var shouldShareHistory: Boolean = false,
        // Store the current outbound session for this room,
        // to avoid re-create and re-share at each startup (if rotation not needed..)
        // This is specific to megolm but not sure how to model it better
        var outboundSessionInfo: OutboundGroupSessionInfoEntity? = null,
        // a security to ensure that a room will never revert to not encrypted
        // even if a new state event with empty encryption, or state is reset somehow
        var wasEncryptedOnce: Boolean? = false
) :
        RealmObject() {

    companion object
}
