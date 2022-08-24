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

import io.realm.kotlin.ext.realmListOf
import io.realm.kotlin.types.RealmList
import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PrimaryKey

internal class UserEntity : RealmObject {
    @PrimaryKey var userId: String? = null
    var devices: RealmList<DeviceInfoEntity> = realmListOf()
    var crossSigningInfoEntity: CrossSigningInfoEntity? = null
    var deviceTrackingStatus: Int = 0

    companion object
}

/*
internal fun UserEntity.deleteOnCascade() {
    devices.clearWith { it.deleteOnCascade() }
    crossSigningInfoEntity?.deleteOnCascade()
    deleteFromRealm()
}

 */
