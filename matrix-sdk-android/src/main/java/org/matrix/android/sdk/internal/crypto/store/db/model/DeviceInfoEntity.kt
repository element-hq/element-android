/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.crypto.store.db.model

import io.realm.RealmObject
import io.realm.RealmResults
import io.realm.annotations.LinkingObjects
import io.realm.annotations.PrimaryKey

internal fun DeviceInfoEntity.Companion.createPrimaryKey(userId: String, deviceId: String) = "$userId|$deviceId"

internal open class DeviceInfoEntity(
        @PrimaryKey var primaryKey: String = "",
        var deviceId: String? = null,
        var identityKey: String? = null,
        var userId: String? = null,
        var isBlocked: Boolean? = null,
        var algorithmListJson: String? = null,
        var keysMapJson: String? = null,
        var signatureMapJson: String? = null,
        // Will contain the device name from unsigned data if present
        var unsignedMapJson: String? = null,
        var trustLevelEntity: TrustLevelEntity? = null,
        /**
         * We use that to make distinction between old devices (there before mine)
         * and new ones. Used for example to detect new unverified login
         */
        var firstTimeSeenLocalTs: Long? = null
) : RealmObject() {

    @LinkingObjects("devices")
    val users: RealmResults<UserEntity>? = null

    companion object
}

internal fun DeviceInfoEntity.deleteOnCascade() {
    trustLevelEntity?.deleteFromRealm()
    deleteFromRealm()
}
