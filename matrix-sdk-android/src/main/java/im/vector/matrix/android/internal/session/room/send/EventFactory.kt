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

package im.vector.matrix.android.internal.session.room.send

import im.vector.matrix.android.api.auth.data.Credentials
import im.vector.matrix.android.api.session.events.model.Content
import im.vector.matrix.android.api.session.events.model.Event
import im.vector.matrix.android.api.session.events.model.EventType
import im.vector.matrix.android.api.session.room.model.message.MessageTextContent
import im.vector.matrix.android.internal.di.MoshiProvider
import im.vector.matrix.android.internal.session.SessionScope
import javax.inject.Inject

internal class EventFactory @Inject constructor(private val credentials: Credentials) {

    private val moshi = MoshiProvider.providesMoshi()

    fun createTextEvent(roomId: String, msgType: String, text: String): Event {
        val content = MessageTextContent(type = msgType, body = text)

        return Event(
                roomId = roomId,
                originServerTs = dummyOriginServerTs(),
                sender = credentials.userId,
                eventId = dummyEventId(roomId),
                type = EventType.MESSAGE,
                content = toContent(content)
        )
    }

    private fun dummyOriginServerTs(): Long {
        return System.currentTimeMillis()
    }

    private fun dummyEventId(roomId: String): String {
        return roomId + "-" + dummyOriginServerTs()
    }

    @Suppress("UNCHECKED_CAST")
    private inline fun <reified T> toContent(data: T?): Content? {
        val moshiAdapter = moshi.adapter(T::class.java)
        val jsonValue = moshiAdapter.toJsonValue(data)
        return jsonValue as? Content?
    }


}
