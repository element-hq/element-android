/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2022 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.database.migration

import io.realm.DynamicRealm
import org.matrix.android.sdk.internal.database.model.EditAggregatedSummaryEntityFields
import org.matrix.android.sdk.internal.database.model.EditionOfEventFields
import org.matrix.android.sdk.internal.util.database.RealmMigrator

internal class MigrateSessionTo008(realm: DynamicRealm) : RealmMigrator(realm, 8) {

    override fun doMigrate(realm: DynamicRealm) {
        val editionOfEventSchema = realm.schema.create("EditionOfEvent")
                .addField(EditionOfEventFields.CONTENT, String::class.java)
                .addField(EditionOfEventFields.EVENT_ID, String::class.java)
                .setRequired(EditionOfEventFields.EVENT_ID, true)
                .addField(EditionOfEventFields.SENDER_ID, String::class.java)
                .setRequired(EditionOfEventFields.SENDER_ID, true)
                .addField(EditionOfEventFields.TIMESTAMP, Long::class.java)
                .addField(EditionOfEventFields.IS_LOCAL_ECHO, Boolean::class.java)

        realm.schema.get("EditAggregatedSummaryEntity")
                ?.removeField("aggregatedContent")
                ?.removeField("sourceEvents")
                ?.removeField("lastEditTs")
                ?.removeField("sourceLocalEchoEvents")
                ?.addRealmListField(EditAggregatedSummaryEntityFields.EDITIONS.`$`, editionOfEventSchema)

        // This has to be done once a parent use the model as a child
        // See https://github.com/realm/realm-java/issues/7402
        editionOfEventSchema.isEmbedded = true
    }
}
