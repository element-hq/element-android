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

internal open class KeysBackupDataEntity(
        // Primary key to update this object. There is only one object, so it's a constant, please do not set it
        @PrimaryKey
        var primaryKey: Int = 0,
        // The last known hash of the backed up keys on the server
        var backupLastServerHash: String? = null,
        // The last known number of backed up keys on the server
        var backupLastServerNumberOfKeys: Int? = null
) : RealmObject()
