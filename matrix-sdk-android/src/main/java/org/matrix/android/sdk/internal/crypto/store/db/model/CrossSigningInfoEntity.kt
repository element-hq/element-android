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
import org.matrix.android.sdk.api.session.crypto.crosssigning.KeyUsage
import org.matrix.android.sdk.internal.extensions.clearWith

internal open class CrossSigningInfoEntity(
        @PrimaryKey
        var userId: String? = null,
        var crossSigningKeys: RealmList<KeyInfoEntity> = RealmList()
) : RealmObject() {

    companion object

    fun getMasterKey() = crossSigningKeys.firstOrNull { it.usages.contains(KeyUsage.MASTER.value) }

    fun setMasterKey(info: KeyInfoEntity?) {
        crossSigningKeys
                .filter { it.usages.contains(KeyUsage.MASTER.value) }
                .forEach { crossSigningKeys.remove(it) }
        info?.let { crossSigningKeys.add(it) }
    }

    fun getSelfSignedKey() = crossSigningKeys.firstOrNull { it.usages.contains(KeyUsage.SELF_SIGNING.value) }

    fun setSelfSignedKey(info: KeyInfoEntity?) {
        crossSigningKeys
                .filter { it.usages.contains(KeyUsage.SELF_SIGNING.value) }
                .forEach { crossSigningKeys.remove(it) }
        info?.let { crossSigningKeys.add(it) }
    }

    fun getUserSigningKey() = crossSigningKeys.firstOrNull { it.usages.contains(KeyUsage.USER_SIGNING.value) }

    fun setUserSignedKey(info: KeyInfoEntity?) {
        crossSigningKeys
                .filter { it.usages.contains(KeyUsage.USER_SIGNING.value) }
                .forEach { crossSigningKeys.remove(it) }
        info?.let { crossSigningKeys.add(it) }
    }
}

internal fun CrossSigningInfoEntity.deleteOnCascade() {
    crossSigningKeys.clearWith { it.deleteOnCascade() }
    deleteFromRealm()
}
