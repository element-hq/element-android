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

internal open class KeyInfoEntity(
        var publicKeyBase64: String? = null,
//        var isTrusted: Boolean = false,
        var usages: RealmList<String> = RealmList(),
        /**
         * The signature of this MXDeviceInfo.
         * A map from "<userId>" to a map from "<key type>:<Publickey>" to "<signature>"
         */
        var signatures: String? = null,
        var trustLevelEntity: TrustLevelEntity? = null
) : RealmObject()

internal fun KeyInfoEntity.deleteOnCascade() {
    trustLevelEntity?.deleteFromRealm()
    deleteFromRealm()
}
