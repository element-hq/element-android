/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.crypto.store.db.model

import io.realm.RealmList
import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import org.matrix.android.sdk.internal.extensions.clearWith

internal open class UserEntity(
        @PrimaryKey var userId: String? = null,
        var devices: RealmList<DeviceInfoEntity> = RealmList(),
        var crossSigningInfoEntity: CrossSigningInfoEntity? = null,
        var deviceTrackingStatus: Int = 0
) : RealmObject() {

    companion object
}

internal fun UserEntity.deleteOnCascade() {
    devices.clearWith { it.deleteOnCascade() }
    crossSigningInfoEntity?.deleteOnCascade()
    deleteFromRealm()
}
