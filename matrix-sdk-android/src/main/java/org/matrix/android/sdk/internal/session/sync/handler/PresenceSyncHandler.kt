/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2021 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.sync.handler

import io.realm.Realm
import org.matrix.android.sdk.api.MatrixConfiguration
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.getPresenceContent
import org.matrix.android.sdk.api.session.sync.model.PresenceSyncResponse
import org.matrix.android.sdk.internal.database.model.RoomMemberSummaryEntity
import org.matrix.android.sdk.internal.database.model.RoomSummaryEntity
import org.matrix.android.sdk.internal.database.model.presence.UserPresenceEntity
import org.matrix.android.sdk.internal.database.query.updateDirectUserPresence
import org.matrix.android.sdk.internal.database.query.updateUserPresence
import javax.inject.Inject

internal class PresenceSyncHandler @Inject constructor(private val matrixConfiguration: MatrixConfiguration) {

    fun handle(realm: Realm, presenceSyncResponse: PresenceSyncResponse?) {
        presenceSyncResponse?.events
                ?.filter { event -> event.type == EventType.PRESENCE }
                ?.forEach { event ->
                    val content = event.getPresenceContent() ?: return@forEach
                    val userId = event.senderId ?: return@forEach
                    val userPresenceEntity = UserPresenceEntity(
                            userId = userId,
                            lastActiveAgo = content.lastActiveAgo,
                            statusMessage = content.statusMessage,
                            isCurrentlyActive = content.isCurrentlyActive,
                            avatarUrl = content.avatarUrl,
                            displayName = content.displayName
                    ).also {
                        it.presence = content.presence
                    }

                    storePresenceToDB(realm, userPresenceEntity)
                }
    }

    /**
     * Store user presence to DB and update Direct Rooms and Room Member Summaries accordingly.
     */
    private fun storePresenceToDB(realm: Realm, userPresenceEntity: UserPresenceEntity) =
            realm.copyToRealmOrUpdate(userPresenceEntity)?.apply {
                RoomSummaryEntity.updateDirectUserPresence(realm, userPresenceEntity.userId, this)
                RoomMemberSummaryEntity.updateUserPresence(realm, userPresenceEntity.userId, this)
            }
}
