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

package im.vector.matrix.android.api.session.events.model

import android.text.TextUtils
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import im.vector.matrix.android.api.session.crypto.MXCryptoError
import im.vector.matrix.android.api.session.room.model.message.MessageContent
import im.vector.matrix.android.api.session.room.model.message.MessageType
import im.vector.matrix.android.api.session.room.send.SendState
import im.vector.matrix.android.api.util.JsonDict
import im.vector.matrix.android.internal.crypto.algorithms.olm.OlmDecryptionResult
import im.vector.matrix.android.internal.di.MoshiProvider
import org.json.JSONObject
import timber.log.Timber

typealias Content = JsonDict

/**
 * This methods is a facility method to map a json content to a model.
 */
inline fun <reified T> Content?.toModel(catchError: Boolean = true): T? {
    return this?.let {
        val moshi = MoshiProvider.providesMoshi()
        val moshiAdapter = moshi.adapter(T::class.java)
        return try {
            moshiAdapter.fromJsonValue(it)
        } catch (e: Exception) {
            if (catchError) {
                Timber.e(e, "To model failed : $e")
                null
            } else {
                throw e
            }
        }
    }
}

/**
 * This methods is a facility method to map a model to a json Content
 */
@Suppress("UNCHECKED_CAST")
inline fun <reified T> T?.toContent(): Content? {
    return this?.let {
        val moshi = MoshiProvider.providesMoshi()
        val moshiAdapter = moshi.adapter(T::class.java)
        return moshiAdapter.toJsonValue(it) as Content
    }
}

/**
 * Generic event class with all possible fields for events.
 * The content and prevContent json fields can easily be mapped to a model with [toModel] method.
 */
@JsonClass(generateAdapter = true)
data class Event(
        @Json(name = "type") val type: String,
        @Json(name = "event_id") val eventId: String? = null,
        @Json(name = "content") val content: Content? = null,
        @Json(name = "prev_content") val prevContent: Content? = null,
        @Json(name = "origin_server_ts") val originServerTs: Long? = null,
        @Json(name = "sender") val senderId: String? = null,
        @Json(name = "state_key") val stateKey: String? = null,
        @Json(name = "room_id") val roomId: String? = null,
        @Json(name = "unsigned") val unsignedData: UnsignedData? = null,
        @Json(name = "redacts") val redacts: String? = null
) {


    @Transient
    var mxDecryptionResult: OlmDecryptionResult? = null

    @Transient
    var mCryptoError: MXCryptoError.ErrorType? = null

    @Transient
    var sendState: SendState = SendState.UNKNOWN


    /**
     * Check if event is a state event.
     * @return true if event is state event.
     */
    fun isStateEvent(): Boolean {
        return EventType.isStateEvent(getClearType())
    }

    //==============================================================================================================
    // Crypto
    //==============================================================================================================

    /**
     * @return true if this event is encrypted.
     */
    fun isEncrypted(): Boolean {
        return TextUtils.equals(type, EventType.ENCRYPTED)
    }

    /**
     * @return The curve25519 key that sent this event.
     */
    fun getSenderKey(): String? {
        return mxDecryptionResult?.senderKey
    }

    /**
     * @return The additional keys the sender of this encrypted event claims to possess.
     */
    fun getKeysClaimed(): Map<String, String> {
        return mxDecryptionResult?.keysClaimed ?: HashMap()
    }

    /**
     * @return the event type
     */
    fun getClearType(): String {
        return mxDecryptionResult?.payload?.get("type")?.toString() ?: type
    }

    /**
     * @return the event content
     */
    fun getClearContent(): Content? {
        return mxDecryptionResult?.payload?.get("content") as? Content ?: content
    }

    fun toContentStringWithIndent(): String {
        val contentMap = toContent()?.toMutableMap() ?: HashMap()
        return JSONObject(contentMap).toString(4)
    }

    fun toClearContentStringWithIndent(): String? {
        val contentMap = this.mxDecryptionResult?.payload?.toMutableMap()
        val adapter = MoshiProvider.providesMoshi().adapter(Map::class.java)
        return contentMap?.let { JSONObject(adapter.toJson(it)).toString(4) }
    }

    /**
     * Tells if the event is redacted
     */
    fun isRedacted() = unsignedData?.redactedEvent != null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Event

        if (type != other.type) return false
        if (eventId != other.eventId) return false
        if (content != other.content) return false
        if (prevContent != other.prevContent) return false
        if (originServerTs != other.originServerTs) return false
        if (senderId != other.senderId) return false
        if (stateKey != other.stateKey) return false
        if (roomId != other.roomId) return false
        if (unsignedData != other.unsignedData) return false
        if (redacts != other.redacts) return false
        if (mxDecryptionResult != other.mxDecryptionResult) return false
        if (mCryptoError != other.mCryptoError) return false
        if (sendState != other.sendState) return false

        return true
    }

    override fun hashCode(): Int {
        var result = type.hashCode()
        result = 31 * result + (eventId?.hashCode() ?: 0)
        result = 31 * result + (content?.hashCode() ?: 0)
        result = 31 * result + (prevContent?.hashCode() ?: 0)
        result = 31 * result + (originServerTs?.hashCode() ?: 0)
        result = 31 * result + (senderId?.hashCode() ?: 0)
        result = 31 * result + (stateKey?.hashCode() ?: 0)
        result = 31 * result + (roomId?.hashCode() ?: 0)
        result = 31 * result + (unsignedData?.hashCode() ?: 0)
        result = 31 * result + (redacts?.hashCode() ?: 0)
        result = 31 * result + (mxDecryptionResult?.hashCode() ?: 0)
        result = 31 * result + (mCryptoError?.hashCode() ?: 0)
        result = 31 * result + sendState.hashCode()
        return result
    }

}


fun Event.isTextMessage(): Boolean {
    return getClearType() == EventType.MESSAGE
            && when (getClearContent()?.toModel<MessageContent>()?.type) {
        MessageType.MSGTYPE_TEXT,
        MessageType.MSGTYPE_EMOTE,
        MessageType.MSGTYPE_NOTICE -> true
        else                       -> false
    }
}

fun Event.isImageMessage(): Boolean {
    return getClearType() == EventType.MESSAGE
            && when (getClearContent()?.toModel<MessageContent>()?.type) {
        MessageType.MSGTYPE_IMAGE -> true
        else                      -> false
    }
}