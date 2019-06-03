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
import im.vector.matrix.android.api.util.JsonDict
import im.vector.matrix.android.internal.crypto.MXEventDecryptionResult
import im.vector.matrix.android.internal.di.MoshiProvider
import timber.log.Timber
import java.util.*

typealias Content = JsonDict

/**
 * This methods is a facility method to map a json content to a model.
 */
inline fun <reified T> Content?.toModel(): T? {
    return this?.let {
        val moshi = MoshiProvider.providesMoshi()
        val moshiAdapter = moshi.adapter(T::class.java)
        return moshiAdapter.fromJsonValue(it)
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
        @Json(name = "sender") val sender: String? = null,
        @Json(name = "state_key") val stateKey: String? = null,
        @Json(name = "room_id") val roomId: String? = null,
        @Json(name = "unsigned") val unsignedData: UnsignedData? = null,
        @Json(name = "redacts") val redacts: String? = null

) {

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
     * For encrypted events, the plaintext payload for the event.
     * This is a small MXEvent instance with typically value for `type` and 'content' fields.
     */
    @Transient
    var mClearEvent: Event? = null
        private set

    /**
     * Curve25519 key which we believe belongs to the sender of the event.
     * See `senderKey` property.
     */
    @Transient
    private var mSenderCurve25519Key: String? = null

    /**
     * Ed25519 key which the sender of this event (for olm) or the creator of the megolm session (for megolm) claims to own.
     * See `claimedEd25519Key` property.
     */
    @Transient
    private var mClaimedEd25519Key: String? = null

    /**
     * Curve25519 keys of devices involved in telling us about the senderCurve25519Key and claimedEd25519Key.
     * See `forwardingCurve25519KeyChain` property.
     */
    @Transient
    private var mForwardingCurve25519KeyChain: List<String> = ArrayList()

    /**
     * Decryption error
     */
    @Transient
    var mCryptoError: MXCryptoError? = null
        private set

    /**
     * @return true if this event is encrypted.
     */
    fun isEncrypted(): Boolean {
        return TextUtils.equals(type, EventType.ENCRYPTED)
    }

    /**
     * Update the clear data on this event.
     * This is used after decrypting an event; it should not be used by applications.
     * It fires kMXEventDidDecryptNotification.
     *
     * @param decryptionResult the decryption result, including the plaintext and some key info.
     */
    internal fun setClearData(decryptionResult: MXEventDecryptionResult?) {
        mClearEvent = null
        if (decryptionResult != null) {
            if (decryptionResult.clearEvent != null) {
                val adapter = MoshiProvider.providesMoshi().adapter(Event::class.java)
                mClearEvent = adapter.fromJsonValue(decryptionResult.clearEvent)

            }
            mClearEvent?.apply {
                mSenderCurve25519Key = decryptionResult.senderCurve25519Key
                mClaimedEd25519Key = decryptionResult.claimedEd25519Key
                mForwardingCurve25519KeyChain = decryptionResult.forwardingCurve25519KeyChain
                try {
                    // Add "m.relates_to" data from e2e event to the unencrypted event
                    // TODO
                    //if (getWireContent().getAsJsonObject().has("m.relates_to")) {
                    //    clearEvent!!.getContentAsJsonObject()
                    //            .add("m.relates_to", getWireContent().getAsJsonObject().get("m.relates_to"))
                    //}
                } catch (e: Exception) {
                    Timber.e(e, "Unable to restore 'm.relates_to' the clear event")
                }
            }
        }
        mCryptoError = null
    }

    /**
     * @return The curve25519 key that sent this event.
     */
    fun getSenderKey(): String? {
        return if (null != mClearEvent) {
            mClearEvent!!.mSenderCurve25519Key
        } else {
            mSenderCurve25519Key
        }
    }

    /**
     * @return The additional keys the sender of this encrypted event claims to possess.
     */
    fun getKeysClaimed(): Map<String, String> {
        val res = HashMap<String, String>()

        val claimedEd25519Key = if (null != mClearEvent) mClearEvent!!.mClaimedEd25519Key else mClaimedEd25519Key

        if (null != claimedEd25519Key) {
            res["ed25519"] = claimedEd25519Key
        }

        return res
    }

    /**
     * @return the event type
     */
    fun getClearType(): String {
        return mClearEvent?.type ?: type
    }

    /**
     * @return the event content
     */
    fun getClearContent(): Content? {
        return mClearEvent?.content ?: content
    }

    /**
     * @return the linked crypto error
     */
    fun getCryptoError(): MXCryptoError? {
        return mCryptoError
    }

    /**
     * Update the linked crypto error
     *
     * @param error the new crypto error.
     */
    fun setCryptoError(error: MXCryptoError?) {
        mCryptoError = error
        if (null != error) {
            mClearEvent = null
        }
    }

}