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
import org.matrix.android.sdk.internal.crypto.store.db.deserializeFromRealm
import org.matrix.android.sdk.internal.crypto.store.db.serializeForRealm
import org.matrix.olm.OlmSession

internal fun OlmSessionEntity.Companion.createPrimaryKey(sessionId: String, deviceKey: String) = "$sessionId|$deviceKey"

// olmSessionData is a serialized OlmSession
internal open class OlmSessionEntity(@PrimaryKey var primaryKey: String = "",
                                     var sessionId: String? = null,
                                     var deviceKey: String? = null,
                                     var olmSessionData: String? = null,
                                     var lastReceivedMessageTs: Long = 0) :
    RealmObject() {

    fun getOlmSession(): OlmSession? {
        return deserializeFromRealm(olmSessionData)
    }

    fun putOlmSession(olmSession: OlmSession?) {
        olmSessionData = serializeForRealm(olmSession)
    }

    companion object
}
