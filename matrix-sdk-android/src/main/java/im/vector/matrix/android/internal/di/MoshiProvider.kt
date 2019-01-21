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
import im.vector.matrix.android.api.session.room.model.message.MessageTextContent
import im.vector.matrix.android.api.session.room.model.message.MessageType
import im.vector.matrix.android.api.session.room.model.message.MessageVideoContent
import im.vector.matrix.android.internal.network.parsing.RuntimeJsonAdapterFactory
import im.vector.matrix.android.internal.network.parsing.UriMoshiAdapter
import im.vector.matrix.android.internal.session.sync.model.UserAccountData
import im.vector.matrix.android.internal.session.sync.model.UserAccountDataDirectMessages
import im.vector.matrix.android.internal.session.sync.model.UserAccountDataFallback

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
            .build()

    fun providesMoshi(): Moshi {
        return moshi
    }

}
