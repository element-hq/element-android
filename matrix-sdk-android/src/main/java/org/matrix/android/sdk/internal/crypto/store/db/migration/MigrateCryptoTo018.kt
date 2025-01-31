/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2022 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.crypto.store.db.migration

import io.realm.DynamicRealm
import org.matrix.android.sdk.internal.crypto.model.InboundGroupSessionData
import org.matrix.android.sdk.internal.crypto.store.db.model.OlmInboundGroupSessionEntityFields
import org.matrix.android.sdk.internal.di.MoshiProvider
import org.matrix.android.sdk.internal.util.database.RealmMigrator
import timber.log.Timber

/**
 * This migration is adding support for trusted flags on megolm sessions.
 * We can't really assert the trust of existing keys, so for the sake of simplicity we are going to
 * mark existing keys as safe.
 * This migration can take long depending on the account
 */
internal class MigrateCryptoTo018(realm: DynamicRealm) : RealmMigrator(realm, 18) {

    private val moshiAdapter = MoshiProvider.providesMoshi().adapter(InboundGroupSessionData::class.java)

    override fun doMigrate(realm: DynamicRealm) {
        realm.schema.get("OlmInboundGroupSessionEntity")
                ?.transform { dynamicObject ->
                    try {
                        dynamicObject.getString(OlmInboundGroupSessionEntityFields.INBOUND_GROUP_SESSION_DATA_JSON)?.let { oldData ->
                            moshiAdapter.fromJson(oldData)?.let { dataToMigrate ->
                                dataToMigrate.copy(trusted = true).let {
                                    dynamicObject.setString(OlmInboundGroupSessionEntityFields.INBOUND_GROUP_SESSION_DATA_JSON, moshiAdapter.toJson(it))
                                }
                            }
                        }
                    } catch (failure: Throwable) {
                        Timber.e(failure, "Failed to migrate megolm session")
                    }
                }
    }
}
