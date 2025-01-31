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

internal open class MyDeviceLastSeenInfoEntity(
        /** The device id. */
        @PrimaryKey var deviceId: String? = null,
        /** The device display name. */
        var displayName: String? = null,
        /** The last time this device has been seen. */
        var lastSeenTs: Long? = null,
        /** The last ip address. */
        var lastSeenIp: String? = null
) : RealmObject() {

    companion object
}
