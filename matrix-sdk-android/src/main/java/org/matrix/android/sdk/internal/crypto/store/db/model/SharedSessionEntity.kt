/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.crypto.store.db.model

import io.realm.RealmObject
import io.realm.annotations.Index

/**
 * Keep a record of to whom (user/device) a given session should have been shared.
 * It will be used to reply to keyshare requests from other users, in order to see if
 * this session was originaly shared with a given user
 */
internal open class SharedSessionEntity(
        var roomId: String? = null,
        var algorithm: String? = null,
        @Index var sessionId: String? = null,
        @Index var userId: String? = null,
        @Index var deviceId: String? = null,
        @Index var deviceIdentityKey: String? = null,
        var chainIndex: Int? = null
) : RealmObject() {

    companion object
}
