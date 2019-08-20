/*
 * Copyright 2019 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.matrix.android.internal.database.mapper

import im.vector.matrix.android.api.session.crypto.CryptoService
import im.vector.matrix.android.api.session.crypto.MXCryptoError
import im.vector.matrix.android.api.session.room.model.RoomSummary
import im.vector.matrix.android.api.session.room.model.tag.RoomTag
import im.vector.matrix.android.internal.crypto.algorithms.olm.OlmDecryptionResult
import im.vector.matrix.android.internal.database.model.RoomSummaryEntity
import java.util.*
import javax.inject.Inject

internal class RoomSummaryMapper @Inject constructor(
        private val cryptoService: CryptoService,
        private val timelineEventMapper: TimelineEventMapper
) {

    fun map(roomSummaryEntity: RoomSummaryEntity): RoomSummary {
        val tags = roomSummaryEntity.tags.map {
            RoomTag(it.tagName, it.tagOrder)
        }

        val latestEvent = roomSummaryEntity.latestEvent?.let {
            timelineEventMapper.map(it)
        }
        if (latestEvent?.root?.isEncrypted() == true && latestEvent.root.mxDecryptionResult == null) {
            //TODO use a global event decryptor? attache to session and that listen to new sessionId?
            //for now decrypt sync
            try {
                val result = cryptoService.decryptEvent(latestEvent.root, latestEvent.root.roomId + UUID.randomUUID().toString())
                    latestEvent.root.mxDecryptionResult =  OlmDecryptionResult(
                            payload = result.clearEvent,
                            senderKey = result.senderCurve25519Key,
                            keysClaimed = result.claimedEd25519Key?.let { mapOf("ed25519" to it) },
                            forwardingCurve25519KeyChain = result.forwardingCurve25519KeyChain
                    )
            } catch (e: MXCryptoError) {

            }
        }
        return RoomSummary(
                roomId = roomSummaryEntity.roomId,
                displayName = roomSummaryEntity.displayName ?: "",
                topic = roomSummaryEntity.topic ?: "",
                avatarUrl = roomSummaryEntity.avatarUrl ?: "",
                isDirect = roomSummaryEntity.isDirect,
                latestEvent = latestEvent,
                otherMemberIds = roomSummaryEntity.otherMemberIds.toList(),
                highlightCount = roomSummaryEntity.highlightCount,
                notificationCount = roomSummaryEntity.notificationCount,
                tags = tags,
                membership = roomSummaryEntity.membership,
                versioningState = roomSummaryEntity.versioningState
        )
    }
}
