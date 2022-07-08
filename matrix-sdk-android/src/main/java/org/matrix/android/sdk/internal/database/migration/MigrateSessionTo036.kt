/*
 * Copyright (c) 2022 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.database.migration

import io.realm.DynamicRealm
import io.realm.RealmList
import org.matrix.android.sdk.api.session.crypto.MXCryptoError
import org.matrix.android.sdk.api.session.events.model.content.WithHeldCode
import org.matrix.android.sdk.internal.database.model.EventEntityFields
import org.matrix.android.sdk.internal.database.model.RoomSummaryEntityFields
import org.matrix.android.sdk.internal.util.database.RealmMigrator

internal class MigrateSessionTo036(realm: DynamicRealm) : RealmMigrator(realm, 36) {

    override fun doMigrate(realm: DynamicRealm) {
        // add initial withheld code as part as the event entity
        realm.schema.get("EventEntity")
                ?.addField(EventEntityFields.DECRYPTION_WITHHELD_CODE, String::class.java)
                ?.transform { dynamicObject ->
                    // previously the withheld code was stored in mCryptoErrorReason when the error type
                    // was KEYS_WITHHELD but now we are storing it properly for both type UNKNOWN_INBOUND_SESSION_ID and UNKNOWN_MESSAGE_INDEX
                    val reasonString = dynamicObject.getString(EventEntityFields.DECRYPTION_ERROR_REASON)
                    val errorCode = dynamicObject.getString(EventEntityFields.DECRYPTION_ERROR_CODE)
                    // try to see if it's a withheld code
                    val tryAsCode = WithHeldCode.fromCode(reasonString)
                    if (errorCode != null && tryAsCode != null) {
                        // ok migrate to proper code
                        dynamicObject.setString(EventEntityFields.DECRYPTION_WITHHELD_CODE, tryAsCode.value)
                        // the former code was WITHHELD, we can't know if it was unknown session or partially withheld, so assume unknown
                        // we could try to see if session is known but is it worth it?
                        dynamicObject.setString(EventEntityFields.DECRYPTION_WITHHELD_CODE, MXCryptoError.ErrorType.UNKNOWN_INBOUND_SESSION_ID.name)
                    }
                }
    }
}
