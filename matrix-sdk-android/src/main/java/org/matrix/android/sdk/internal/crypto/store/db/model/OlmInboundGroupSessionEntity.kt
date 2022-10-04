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

import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import org.matrix.android.sdk.internal.crypto.model.InboundGroupSessionData
import org.matrix.android.sdk.internal.crypto.model.MXInboundMegolmSessionWrapper
import org.matrix.android.sdk.internal.crypto.store.db.deserializeFromRealm
import org.matrix.android.sdk.internal.crypto.store.db.serializeForRealm
import org.matrix.android.sdk.internal.di.MoshiProvider
import org.matrix.olm.OlmInboundGroupSession
import timber.log.Timber

internal fun OlmInboundGroupSessionEntity.Companion.createPrimaryKey(sessionId: String?, senderKey: String?) = "$sessionId|$senderKey"

internal open class OlmInboundGroupSessionEntity(
        // Combined value to build a primary key
        @PrimaryKey var primaryKey: String? = null,

        // denormalization for faster querying (these fields are in the inboundGroupSessionDataJson)
        var sessionId: String? = null,
        var senderKey: String? = null,
        var roomId: String? = null,

        // Deprecated, used for migration / olmInboundGroupSessionData contains Json
        // keep it in case of problem to have a chance to recover
        var olmInboundGroupSessionData: String? = null,

        // Stores the session data in an extensible format
        // to allow to store data not yet supported for later use
        var inboundGroupSessionDataJson: String? = null,

        // The pickled session
        var serializedOlmInboundGroupSession: String? = null,

        // Flag that indicates whether or not the current inboundSession will be shared to
        // invited users to decrypt past messages
        var sharedHistory: Boolean = false,
        // Indicate if the key has been backed up to the homeserver
        var backedUp: Boolean = false
) :
        RealmObject() {

    fun store(wrapper: MXInboundMegolmSessionWrapper) {
        this.serializedOlmInboundGroupSession = serializeForRealm(wrapper.session)
        this.inboundGroupSessionDataJson = adapter.toJson(wrapper.sessionData)
        this.roomId = wrapper.sessionData.roomId
        this.senderKey = wrapper.sessionData.senderKey
        this.sessionId = wrapper.session.sessionIdentifier()
        this.sharedHistory = wrapper.sessionData.sharedHistory
    }
//    fun getInboundGroupSession(): OlmInboundGroupSessionWrapper2? {
//        return try {
//            deserializeFromRealm<OlmInboundGroupSessionWrapper2?>(olmInboundGroupSessionData)
//        } catch (failure: Throwable) {
//            Timber.e(failure, "## Deserialization failure")
//            return null
//        }
//    }
//
//    fun putInboundGroupSession(olmInboundGroupSessionWrapper: OlmInboundGroupSessionWrapper2?) {
//        olmInboundGroupSessionData = serializeForRealm(olmInboundGroupSessionWrapper)
//    }

    fun getOlmGroupSession(): OlmInboundGroupSession? {
        return try {
            deserializeFromRealm(serializedOlmInboundGroupSession)
        } catch (failure: Throwable) {
            Timber.e(failure, "## Deserialization failure")
            return null
        }
    }

    fun getData(): InboundGroupSessionData? {
        return try {
            inboundGroupSessionDataJson?.let {
                adapter.fromJson(it)
            }
        } catch (failure: Throwable) {
            Timber.e(failure, "## Deserialization failure")
            return null
        }
    }

    fun toModel(): MXInboundMegolmSessionWrapper? {
        val data = getData() ?: return null
        val session = getOlmGroupSession() ?: return null
        return MXInboundMegolmSessionWrapper(
                session = session,
                sessionData = data
        )
    }

    companion object {
        private val adapter = MoshiProvider.providesMoshi()
                .adapter(InboundGroupSessionData::class.java)
    }
}
