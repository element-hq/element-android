/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
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

import com.squareup.moshi.JsonDataException
import io.realm.RealmObject
import io.realm.annotations.Index
import org.matrix.android.sdk.api.session.crypto.MXCryptoError
import org.matrix.android.sdk.api.session.crypto.model.MXEventDecryptionResult
import org.matrix.android.sdk.api.session.crypto.model.OlmDecryptionResult
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.room.send.SendState
import org.matrix.android.sdk.internal.database.mapper.ContentMapper
import org.matrix.android.sdk.internal.di.MoshiProvider
import timber.log.Timber

/**
 * Keep track of gossiping event received in toDevice messages
 * (room key request, or sss secret sharing, as well as cancellations)
 *
 */
internal open class GossipingEventEntity(@Index var type: String? = "",
                                         var content: String? = null,
                                         @Index var sender: String? = null,
                                         var decryptionResultJson: String? = null,
                                         var decryptionErrorCode: String? = null,
                                         var ageLocalTs: Long? = null) : RealmObject() {

    private var sendStateStr: String = SendState.UNKNOWN.name

    var sendState: SendState
        get() {
            return SendState.valueOf(sendStateStr)
        }
        set(value) {
            sendStateStr = value.name
        }

    companion object

    fun setDecryptionResult(result: MXEventDecryptionResult) {
        val decryptionResult = OlmDecryptionResult(
                payload = result.clearEvent,
                senderKey = result.senderCurve25519Key,
                keysClaimed = result.claimedEd25519Key?.let { mapOf("ed25519" to it) },
                forwardingCurve25519KeyChain = result.forwardingCurve25519KeyChain
        )
        val adapter = MoshiProvider.providesMoshi().adapter<OlmDecryptionResult>(OlmDecryptionResult::class.java)
        decryptionResultJson = adapter.toJson(decryptionResult)
        decryptionErrorCode = null
    }

    fun toModel(): Event {
        return Event(
                type = this.type ?: "",
                content = ContentMapper.map(this.content),
                senderId = this.sender
        ).also {
            it.ageLocalTs = this.ageLocalTs
            it.sendState = this.sendState
            this.decryptionResultJson?.let { json ->
                try {
                    it.mxDecryptionResult = MoshiProvider.providesMoshi().adapter(OlmDecryptionResult::class.java).fromJson(json)
                } catch (t: JsonDataException) {
                    Timber.e(t, "Failed to parse decryption result")
                }
            }
            // TODO get the full crypto error object
            it.mCryptoError = this.decryptionErrorCode?.let { errorCode ->
                MXCryptoError.ErrorType.valueOf(errorCode)
            }
        }
    }
}
