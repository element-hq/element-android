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
import im.vector.matrix.android.api.session.room.model.message.*
import im.vector.matrix.android.internal.network.parsing.RuntimeJsonAdapterFactory
import im.vector.matrix.android.internal.network.parsing.UriMoshiAdapter
import im.vector.matrix.android.internal.session.sync.model.UserAccountData
import im.vector.matrix.android.internal.session.sync.model.UserAccountDataDirectMessages
import im.vector.matrix.android.internal.session.sync.model.UserAccountDataFallback
import im.vector.matrix.android.internal.util.JsonCanonicalizer


object MoshiProvider {

    private val moshi: Moshi = Moshi.Builder()
            .add(UriMoshiAdapter())
            .add(RuntimeJsonAdapterFactory.of(UserAccountData::class.java, "type", UserAccountDataFallback::class.java)
                    .registerSubtype(UserAccountDataDirectMessages::class.java, UserAccountData.TYPE_DIRECT_MESSAGES)
            )
            .add(RuntimeJsonAdapterFactory.of(MessageContent::class.java, "msgtype", MessageDefaultContent::class.java)
                    .registerSubtype(MessageTextContent::class.java, MessageType.MSGTYPE_TEXT)
                    .registerSubtype(MessageNoticeContent::class.java, MessageType.MSGTYPE_NOTICE)
                    .registerSubtype(MessageEmoteContent::class.java, MessageType.MSGTYPE_EMOTE)
                    .registerSubtype(MessageAudioContent::class.java, MessageType.MSGTYPE_AUDIO)
                    .registerSubtype(MessageImageContent::class.java, MessageType.MSGTYPE_IMAGE)
                    .registerSubtype(MessageVideoContent::class.java, MessageType.MSGTYPE_VIDEO)
                    .registerSubtype(MessageLocationContent::class.java, MessageType.MSGTYPE_LOCATION)
                    .registerSubtype(MessageFileContent::class.java, MessageType.MSGTYPE_FILE)
            )
            .add(SerializeNulls.JSON_ADAPTER_FACTORY)
            .build()

    fun providesMoshi(): Moshi {
        return moshi
    }

    // TODO Move
    fun <T> getCanonicalJson(type: Class<T>, o: T): String {
        val adapter = moshi.adapter<T>(type)

        val json = adapter.toJson(o)

        // Canonicalize manually
        val can = JsonCanonicalizer.canonicalize(json)

        val jsonSafe = can.replace("\\/", "/")

        return jsonSafe
    }
}


