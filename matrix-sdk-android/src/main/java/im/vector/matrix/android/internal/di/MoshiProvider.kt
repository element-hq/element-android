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

package im.vector.matrix.android.internal.di

import com.squareup.moshi.Moshi
import im.vector.matrix.android.api.session.room.model.message.MessageAudioContent
import im.vector.matrix.android.api.session.room.model.message.MessageContent
import im.vector.matrix.android.api.session.room.model.message.MessageDefaultContent
import im.vector.matrix.android.api.session.room.model.message.MessageEmoteContent
import im.vector.matrix.android.api.session.room.model.message.MessageFileContent
import im.vector.matrix.android.api.session.room.model.message.MessageImageContent
import im.vector.matrix.android.api.session.room.model.message.MessageLocationContent
import im.vector.matrix.android.api.session.room.model.message.MessageNoticeContent
import im.vector.matrix.android.api.session.room.model.message.MessageOptionsContent
import im.vector.matrix.android.api.session.room.model.message.MessagePollResponseContent
import im.vector.matrix.android.api.session.room.model.message.MessageTextContent
import im.vector.matrix.android.api.session.room.model.message.MessageType
import im.vector.matrix.android.api.session.room.model.message.MessageVerificationRequestContent
import im.vector.matrix.android.api.session.room.model.message.MessageVideoContent
import im.vector.matrix.android.internal.network.parsing.ForceToBooleanJsonAdapter
import im.vector.matrix.android.internal.network.parsing.RuntimeJsonAdapterFactory
import im.vector.matrix.android.internal.network.parsing.UriMoshiAdapter

object MoshiProvider {

    private val moshi: Moshi = Moshi.Builder()
            .add(UriMoshiAdapter())
            .add(ForceToBooleanJsonAdapter())
            .add(RuntimeJsonAdapterFactory.of(MessageContent::class.java, "msgtype", MessageDefaultContent::class.java)
                    .registerSubtype(MessageTextContent::class.java, MessageType.MSGTYPE_TEXT)
                    .registerSubtype(MessageNoticeContent::class.java, MessageType.MSGTYPE_NOTICE)
                    .registerSubtype(MessageEmoteContent::class.java, MessageType.MSGTYPE_EMOTE)
                    .registerSubtype(MessageAudioContent::class.java, MessageType.MSGTYPE_AUDIO)
                    .registerSubtype(MessageImageContent::class.java, MessageType.MSGTYPE_IMAGE)
                    .registerSubtype(MessageVideoContent::class.java, MessageType.MSGTYPE_VIDEO)
                    .registerSubtype(MessageLocationContent::class.java, MessageType.MSGTYPE_LOCATION)
                    .registerSubtype(MessageFileContent::class.java, MessageType.MSGTYPE_FILE)
                    .registerSubtype(MessageVerificationRequestContent::class.java, MessageType.MSGTYPE_VERIFICATION_REQUEST)
                    .registerSubtype(MessageOptionsContent::class.java, MessageType.MSGTYPE_OPTIONS)
                    .registerSubtype(MessagePollResponseContent::class.java, MessageType.MSGTYPE_RESPONSE)
            )
            .add(SerializeNulls.JSON_ADAPTER_FACTORY)
            .build()

    fun providesMoshi(): Moshi {
        return moshi
    }
}
