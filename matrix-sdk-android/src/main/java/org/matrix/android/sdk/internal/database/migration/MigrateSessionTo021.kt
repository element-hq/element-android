/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2022 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.database.migration

import io.realm.DynamicRealm
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.content.EncryptionEventContent
import org.matrix.android.sdk.internal.database.model.CurrentStateEventEntityFields
import org.matrix.android.sdk.internal.database.model.EventEntityFields
import org.matrix.android.sdk.internal.database.model.RoomSummaryEntityFields
import org.matrix.android.sdk.internal.di.MoshiProvider
import org.matrix.android.sdk.internal.util.database.RealmMigrator

internal class MigrateSessionTo021(realm: DynamicRealm) : RealmMigrator(realm, 21) {

    override fun doMigrate(realm: DynamicRealm) {
        realm.schema.get("RoomSummaryEntity")
                ?.addField(RoomSummaryEntityFields.E2E_ALGORITHM, String::class.java)
                ?.transform { obj ->

                    val encryptionContentAdapter = MoshiProvider.providesMoshi().adapter(EncryptionEventContent::class.java)

                    val encryptionEvent = realm.where("CurrentStateEventEntity")
                            .equalTo(CurrentStateEventEntityFields.ROOM_ID, obj.getString(RoomSummaryEntityFields.ROOM_ID))
                            .equalTo(CurrentStateEventEntityFields.TYPE, EventType.STATE_ROOM_ENCRYPTION)
                            .findFirst()

                    val encryptionEventRoot = encryptionEvent?.getObject(CurrentStateEventEntityFields.ROOT.`$`)
                    val algorithm = encryptionEventRoot
                            ?.getString(EventEntityFields.CONTENT)?.let {
                                encryptionContentAdapter.fromJson(it)?.algorithm
                            }

                    obj.setString(RoomSummaryEntityFields.E2E_ALGORITHM, algorithm)
                    obj.setBoolean(RoomSummaryEntityFields.IS_ENCRYPTED, encryptionEvent != null)
                    encryptionEventRoot?.getLong(EventEntityFields.ORIGIN_SERVER_TS)?.let {
                        obj.setLong(RoomSummaryEntityFields.ENCRYPTION_EVENT_TS, it)
                    }
                }
    }
}
