/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.matrix.android.sdk.internal.database

import io.realm.DynamicRealm
import io.realm.FieldAttribute
import io.realm.RealmMigration
import org.matrix.android.sdk.api.session.room.model.tag.RoomTag
import org.matrix.android.sdk.internal.database.model.EditAggregatedSummaryEntityFields
import org.matrix.android.sdk.internal.database.model.EditionOfEventFields
import org.matrix.android.sdk.internal.database.model.EventEntityFields
import org.matrix.android.sdk.internal.database.model.HomeServerCapabilitiesEntityFields
import org.matrix.android.sdk.internal.database.model.PendingThreePidEntityFields
import org.matrix.android.sdk.internal.database.model.PreviewUrlCacheEntityFields
import org.matrix.android.sdk.internal.database.model.RoomEntityFields
import org.matrix.android.sdk.internal.database.model.RoomMembersLoadStatusType
import org.matrix.android.sdk.internal.database.model.RoomSummaryEntityFields
import org.matrix.android.sdk.internal.database.model.RoomTagEntityFields
import org.matrix.android.sdk.internal.database.model.TimelineEventEntityFields
import timber.log.Timber
import javax.inject.Inject

class RealmSessionStoreMigration @Inject constructor() : RealmMigration {

    companion object {
        const val SESSION_STORE_SCHEMA_VERSION = 9L
    }

    override fun migrate(realm: DynamicRealm, oldVersion: Long, newVersion: Long) {
        Timber.v("Migrating Realm Session from $oldVersion to $newVersion")

        if (oldVersion <= 0) migrateTo1(realm)
        if (oldVersion <= 1) migrateTo2(realm)
        if (oldVersion <= 2) migrateTo3(realm)
        if (oldVersion <= 3) migrateTo4(realm)
        if (oldVersion <= 4) migrateTo5(realm)
        if (oldVersion <= 5) migrateTo6(realm)
        if (oldVersion <= 6) migrateTo7(realm)
        if (oldVersion <= 7) migrateTo8(realm)
        if (oldVersion <= 8) migrateTo9(realm)
    }

    private fun migrateTo1(realm: DynamicRealm) {
        Timber.d("Step 0 -> 1")
        // Add hasFailedSending in RoomSummary and a small warning icon on room list

        realm.schema.get("RoomSummaryEntity")
                ?.addField(RoomSummaryEntityFields.HAS_FAILED_SENDING, Boolean::class.java)
                ?.transform { obj ->
                    obj.setBoolean(RoomSummaryEntityFields.HAS_FAILED_SENDING, false)
                }
    }

    private fun migrateTo2(realm: DynamicRealm) {
        Timber.d("Step 1 -> 2")
        realm.schema.get("HomeServerCapabilitiesEntity")
                ?.addField("adminE2EByDefault", Boolean::class.java)
                ?.transform { obj ->
                    obj.setBoolean("adminE2EByDefault", true)
                }
    }

    private fun migrateTo3(realm: DynamicRealm) {
        Timber.d("Step 2 -> 3")
        realm.schema.get("HomeServerCapabilitiesEntity")
                ?.addField("preferredJitsiDomain", String::class.java)
                ?.transform { obj ->
                    // Schedule a refresh of the capabilities
                    obj.setLong(HomeServerCapabilitiesEntityFields.LAST_UPDATED_TIMESTAMP, 0)
                }
    }

    private fun migrateTo4(realm: DynamicRealm) {
        Timber.d("Step 3 -> 4")
        realm.schema.create("PendingThreePidEntity")
                .addField(PendingThreePidEntityFields.CLIENT_SECRET, String::class.java)
                .setRequired(PendingThreePidEntityFields.CLIENT_SECRET, true)
                .addField(PendingThreePidEntityFields.EMAIL, String::class.java)
                .addField(PendingThreePidEntityFields.MSISDN, String::class.java)
                .addField(PendingThreePidEntityFields.SEND_ATTEMPT, Int::class.java)
                .addField(PendingThreePidEntityFields.SID, String::class.java)
                .setRequired(PendingThreePidEntityFields.SID, true)
                .addField(PendingThreePidEntityFields.SUBMIT_URL, String::class.java)
    }

    private fun migrateTo5(realm: DynamicRealm) {
        Timber.d("Step 4 -> 5")
        realm.schema.get("HomeServerCapabilitiesEntity")
                ?.removeField("adminE2EByDefault")
                ?.removeField("preferredJitsiDomain")
    }

    private fun migrateTo6(realm: DynamicRealm) {
        Timber.d("Step 5 -> 6")
        realm.schema.create("PreviewUrlCacheEntity")
                .addField(PreviewUrlCacheEntityFields.URL, String::class.java)
                .setRequired(PreviewUrlCacheEntityFields.URL, true)
                .addPrimaryKey(PreviewUrlCacheEntityFields.URL)
                .addField(PreviewUrlCacheEntityFields.URL_FROM_SERVER, String::class.java)
                .addField(PreviewUrlCacheEntityFields.SITE_NAME, String::class.java)
                .addField(PreviewUrlCacheEntityFields.TITLE, String::class.java)
                .addField(PreviewUrlCacheEntityFields.DESCRIPTION, String::class.java)
                .addField(PreviewUrlCacheEntityFields.MXC_URL, String::class.java)
                .addField(PreviewUrlCacheEntityFields.LAST_UPDATED_TIMESTAMP, Long::class.java)
    }

    private fun migrateTo7(realm: DynamicRealm) {
        Timber.d("Step 6 -> 7")
        realm.schema.get("RoomEntity")
                ?.addField(RoomEntityFields.MEMBERS_LOAD_STATUS_STR, String::class.java)
                ?.transform { obj ->
                    if (obj.getBoolean("areAllMembersLoaded")) {
                        obj.setString("membersLoadStatusStr", RoomMembersLoadStatusType.LOADED.name)
                    } else {
                        obj.setString("membersLoadStatusStr", RoomMembersLoadStatusType.NONE.name)
                    }
                }
                ?.removeField("areAllMembersLoaded")
    }

    private fun migrateTo8(realm: DynamicRealm) {
        Timber.d("Step 7 -> 8")

        val editionOfEventSchema = realm.schema.create("EditionOfEvent")
                .apply {
                    // setEmbedded does not return `this`...
                    isEmbedded = true
                }
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
    }

    fun migrateTo9(realm: DynamicRealm) {
        Timber.d("Step 8 -> 9")

        realm.schema.get("RoomSummaryEntity")
                ?.addField(RoomSummaryEntityFields.LAST_ACTIVITY_TIME, Long::class.java, FieldAttribute.INDEXED)
                ?.setNullable(RoomSummaryEntityFields.LAST_ACTIVITY_TIME, true)
                ?.addIndex(RoomSummaryEntityFields.MEMBERSHIP_STR)
                ?.addIndex(RoomSummaryEntityFields.IS_DIRECT)
                ?.addIndex(RoomSummaryEntityFields.VERSIONING_STATE_STR)

                ?.addField(RoomSummaryEntityFields.IS_FAVOURITE, Boolean::class.java)
                ?.addIndex(RoomSummaryEntityFields.IS_FAVOURITE)
                ?.addField(RoomSummaryEntityFields.IS_LOW_PRIORITY, Boolean::class.java)
                ?.addIndex(RoomSummaryEntityFields.IS_LOW_PRIORITY)
                ?.addField(RoomSummaryEntityFields.IS_SERVER_NOTICE, Boolean::class.java)
                ?.addIndex(RoomSummaryEntityFields.IS_SERVER_NOTICE)

                ?.transform { obj ->

                    val isFavorite = obj.getList(RoomSummaryEntityFields.TAGS.`$`).any {
                        it.getString(RoomTagEntityFields.TAG_NAME) == RoomTag.ROOM_TAG_FAVOURITE
                    }
                    obj.setBoolean(RoomSummaryEntityFields.IS_FAVOURITE, isFavorite)

                    val isLowPriority = obj.getList(RoomSummaryEntityFields.TAGS.`$`).any {
                        it.getString(RoomTagEntityFields.TAG_NAME) == RoomTag.ROOM_TAG_LOW_PRIORITY
                    }

                    obj.setBoolean(RoomSummaryEntityFields.IS_LOW_PRIORITY, isLowPriority)

//                    XXX migrate last message origin server ts
                    obj.getObject(RoomSummaryEntityFields.LATEST_PREVIEWABLE_EVENT.`$`)
                            ?.getObject(TimelineEventEntityFields.ROOT.`$`)
                            ?.getLong(EventEntityFields.ORIGIN_SERVER_TS)?.let {
                                obj.setLong(RoomSummaryEntityFields.LAST_ACTIVITY_TIME, it)
                            }
                }
    }
}
