/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.api.session.events.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.matrix.android.sdk.api.session.crypto.MXCryptoError
import org.matrix.android.sdk.api.session.room.model.message.MessageContent
import org.matrix.android.sdk.api.session.room.model.message.MessageType
import org.matrix.android.sdk.api.session.room.model.relation.RelationDefaultContent
import org.matrix.android.sdk.api.session.room.send.SendState
import org.matrix.android.sdk.api.util.JsonDict
import org.matrix.android.sdk.internal.crypto.algorithms.olm.OlmDecryptionResult
import org.matrix.android.sdk.internal.crypto.model.event.EncryptedEventContent
import org.matrix.android.sdk.internal.di.MoshiProvider
import org.json.JSONObject
import org.matrix.android.sdk.api.extensions.tryOrNull
import org.matrix.android.sdk.api.failure.MatrixError
import timber.log.Timber

typealias Content = JsonDict

/**
 * This methods is a facility method to map a json content to a model.
 */
inline fun <reified T> Content?.toModel(catchError: Boolean = true): T? {
    val moshi = MoshiProvider.providesMoshi()
    val moshiAdapter = moshi.adapter(T::class.java)
    return try {
        moshiAdapter.fromJsonValue(this)
    } catch (e: Exception) {
        if (catchError) {
            Timber.e(e, "To model failed : $e")
            null
        } else {
            throw e
        }
    }
}

/**
 * This methods is a facility method to map a model to a json Content
 */
@Suppress("UNCHECKED_CAST")
inline fun <reified T> T.toContent(): Content {
    val moshi = MoshiProvider.providesMoshi()
    val moshiAdapter = moshi.adapter(T::class.java)
    return moshiAdapter.toJsonValue(this) as Content
}

/**
 * Generic event class with all possible fields for events.
 * The content and prevContent json fields can easily be mapped to a model with [toModel] method.
 */
@JsonClass(generateAdapter = true)
data class Event(
        @Json(name = "type") val type: String? = null,
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
    var mCryptoErrorReason: String? = null

    @Transient
    var sendState: SendState = SendState.UNKNOWN

    @Transient
    var sendStateDetails: String? = null

    fun sendStateError(): MatrixError? {
        return sendStateDetails?.let {
            val matrixErrorAdapter = MoshiProvider.providesMoshi().adapter(MatrixError::class.java)
            tryOrNull { matrixErrorAdapter.fromJson(it) }
        }
    }

    /**
     * The `age` value transcoded in a timestamp based on the device clock when the SDK received
     * the event from the home server.
     * Unlike `age`, this value is static.
     */
    @Transient
    var ageLocalTs: Long? = null

    /**
     * Copy all fields, including transient fields
     */
    fun copyAll(): Event {
        return copy().also {
            it.mxDecryptionResult = mxDecryptionResult
            it.mCryptoError = mCryptoError
            it.mCryptoErrorReason = mCryptoErrorReason
            it.sendState = sendState
            it.ageLocalTs = ageLocalTs
        }
    }

    /**
     * Check if event is a state event.
     * @return true if event is state event.
     */
    fun isStateEvent(): Boolean {
        return stateKey != null
    }

    // ==============================================================================================================
    // Crypto
    // ==============================================================================================================

    /**
     * @return true if this event is encrypted.
     */
    fun isEncrypted(): Boolean {
        return type == EventType.ENCRYPTED
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
        return mxDecryptionResult?.payload?.get("type")?.toString() ?: type ?: EventType.MISSING_TYPE
    }

    /**
     * @return the event content
     */
    fun getClearContent(): Content? {
        @Suppress("UNCHECKED_CAST")
        return mxDecryptionResult?.payload?.get("content") as? Content ?: content
    }

    fun toContentStringWithIndent(): String {
        val contentMap = toContent()
        return JSONObject(contentMap).toString(4)
    }

    fun toClearContentStringWithIndent(): String? {
        val contentMap = this.mxDecryptionResult?.payload
        val adapter = MoshiProvider.providesMoshi().adapter(Map::class.java)
        return contentMap?.let { JSONObject(adapter.toJson(it)).toString(4) }
    }

    /**
     * Tells if the event is redacted
     */
    fun isRedacted() = unsignedData?.redactedEvent != null

    /**
     * Tells if the event is redacted by the user himself.
     */
    fun isRedactedBySameUser() = senderId == unsignedData?.redactedEvent?.senderId

    fun resolvedPrevContent(): Content? = prevContent ?: unsignedData?.prevContent

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
        if (mCryptoErrorReason != other.mCryptoErrorReason) return false
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
        result = 31 * result + (mCryptoErrorReason?.hashCode() ?: 0)
        result = 31 * result + sendState.hashCode()
        return result
    }
}

fun Event.isTextMessage(): Boolean {
    return getClearType() == EventType.MESSAGE
            && when (getClearContent()?.toModel<MessageContent>()?.msgType) {
        MessageType.MSGTYPE_TEXT,
        MessageType.MSGTYPE_EMOTE,
        MessageType.MSGTYPE_NOTICE -> true
        else                       -> false
    }
}

fun Event.isImageMessage(): Boolean {
    return getClearType() == EventType.MESSAGE
            && when (getClearContent()?.toModel<MessageContent>()?.msgType) {
        MessageType.MSGTYPE_IMAGE -> true
        else                      -> false
    }
}

fun Event.isVideoMessage(): Boolean {
    return getClearType() == EventType.MESSAGE
            && when (getClearContent()?.toModel<MessageContent>()?.msgType) {
        MessageType.MSGTYPE_VIDEO -> true
        else                      -> false
    }
}

fun Event.isAudioMessage(): Boolean {
    return getClearType() == EventType.MESSAGE
            && when (getClearContent()?.toModel<MessageContent>()?.msgType) {
        MessageType.MSGTYPE_AUDIO -> true
        else                      -> false
    }
}

fun Event.isFileMessage(): Boolean {
    return getClearType() == EventType.MESSAGE
            && when (getClearContent()?.toModel<MessageContent>()?.msgType) {
        MessageType.MSGTYPE_FILE -> true
        else                     -> false
    }
}

fun Event.isAttachmentMessage(): Boolean {
    return getClearType() == EventType.MESSAGE
            && when (getClearContent()?.toModel<MessageContent>()?.msgType) {
        MessageType.MSGTYPE_IMAGE,
        MessageType.MSGTYPE_AUDIO,
        MessageType.MSGTYPE_VIDEO,
        MessageType.MSGTYPE_FILE -> true
        else                     -> false
    }
}

fun Event.getRelationContent(): RelationDefaultContent? {
    return if (isEncrypted()) {
        content.toModel<EncryptedEventContent>()?.relatesTo
    } else {
        content.toModel<MessageContent>()?.relatesTo
    }
}

fun Event.isReply(): Boolean {
    return getRelationContent()?.inReplyTo?.eventId != null
}

fun Event.isEdition(): Boolean {
    return getRelationContent()?.takeIf { it.type == RelationType.REPLACE }?.eventId != null
}
