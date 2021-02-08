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

import io.realm.RealmList
import io.realm.RealmObject

internal open class KeyInfoEntity(
        var publicKeyBase64: String? = null,
//        var isTrusted: Boolean = false,
        var usages: RealmList<String> = RealmList(),
        /**
         * The signature of this MXDeviceInfo.
         * A map from "<userId>" to a map from "<key type>:<Publickey>" to "<signature>"
         */
        var signatures: String? = null,
        var trustLevelEntity: TrustLevelEntity? = null
) : RealmObject()

internal fun KeyInfoEntity.deleteOnCascade() {
    trustLevelEntity?.deleteFromRealm()
    deleteFromRealm()
}
