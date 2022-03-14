/*
 * Copyright (c) 2022 New Vector Ltd
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

package org.matrix.android.sdk.internal.crypto.store.db.model

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Types
import io.realm.RealmList
import io.realm.RealmObject
import io.realm.annotations.Index
import org.matrix.android.sdk.api.extensions.tryOrNull
import org.matrix.android.sdk.api.session.crypto.model.OutgoingRoomKeyRequestState
import org.matrix.android.sdk.api.session.crypto.model.RoomKeyRequestBody
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.content.RoomKeyWithHeldContent
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.internal.crypto.OutgoingKeyRequest
import org.matrix.android.sdk.internal.crypto.RequestReply
import org.matrix.android.sdk.internal.crypto.RequestResult
import org.matrix.android.sdk.internal.di.MoshiProvider
import timber.log.Timber

internal open class OutgoingKeyRequestEntity(
        @Index var requestId: String? = null,
        var recipientsData: String? = null,
        var requestedInfoStr: String? = null,
        var creationTimeStamp: Long? = null,
        // de-normalization for better query (if not have to query all and parse json)
        @Index var roomId: String? = null,
        @Index var megolmSessionId: String? = null,

        var replies: RealmList<KeyRequestReplyEntity> = RealmList()
) : RealmObject() {

    @Index private var requestStateStr: String = OutgoingRoomKeyRequestState.UNSENT.name

    companion object {

        private val recipientsDataMapper: JsonAdapter<Map<String, List<String>>> =
                MoshiProvider
                        .providesMoshi()
                        .adapter(
                                Types.newParameterizedType(Map::class.java, String::class.java, List::class.java)
                        )
    }

    fun getRequestedKeyInfo(): RoomKeyRequestBody? = RoomKeyRequestBody.fromJson(requestedInfoStr)

    fun setRequestBody(body: RoomKeyRequestBody) {
        requestedInfoStr = body.toJson()
        roomId = body.roomId
        megolmSessionId = body.sessionId
    }

    var requestState: OutgoingRoomKeyRequestState
        get() {
            return tryOrNull { OutgoingRoomKeyRequestState.valueOf(requestStateStr) }
                    ?: OutgoingRoomKeyRequestState.UNSENT
        }
        set(value) {
            requestStateStr = value.name
        }

    private fun getRecipients(): Map<String, List<String>>? {
        return this.recipientsData?.let { recipientsDataMapper.fromJson(it) }
    }

    fun setRecipients(recipients: Map<String, List<String>>) {
        this.recipientsData = recipientsDataMapper.toJson(recipients)
    }

    fun addReply(userId: String, fromDevice: String?, event: Event) {
        Timber.w("##VALR: add reply $userId / $fromDevice  / $event")
        val newReply = KeyRequestReplyEntity(
                senderId = userId,
                fromDevice = fromDevice,
                eventJson = MoshiProvider.providesMoshi().adapter(Event::class.java).toJson(event)
        )
        replies.add(newReply)
    }

    fun toOutgoingGossipingRequest(): OutgoingKeyRequest {
        return OutgoingKeyRequest(
                requestBody = getRequestedKeyInfo(),
                recipients = getRecipients().orEmpty(),
                requestId = requestId ?: "",
                state = requestState,
                results = replies.mapNotNull { entity ->
                    val userId = entity.senderId ?: return@mapNotNull null
                    val result = entity.eventJson?.let {
                        MoshiProvider.providesMoshi().adapter(Event::class.java).fromJson(it)
                    }?.let { event ->
                        eventToResult(event)
                    } ?: return@mapNotNull null
                    RequestReply(
                            userId,
                            entity.fromDevice,
                            result
                    )
                }
        )
    }

    private fun eventToResult(event: Event): RequestResult? {
        return when (event.getClearType()) {
            EventType.ROOM_KEY_WITHHELD  -> {
                event.content.toModel<RoomKeyWithHeldContent>()?.code?.let {
                    RequestResult.Failure(it)
                }
            }
            EventType.FORWARDED_ROOM_KEY -> {
                RequestResult.Success
            }
            else                         -> null
        }
    }
}

internal fun OutgoingKeyRequestEntity.deleteOnCascade() {
    replies.deleteAllFromRealm()
    deleteFromRealm()
}
