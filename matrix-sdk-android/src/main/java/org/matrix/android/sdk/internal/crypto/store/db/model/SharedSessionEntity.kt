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
import io.realm.annotations.Index

/**
 * Keep a record of to whom (user/device) a given session should have been shared.
 * It will be used to reply to keyshare requests from other users, in order to see if
 * this session was originaly shared with a given user
 */
internal open class SharedSessionEntity(
        var roomId: String? = null,
        var algorithm: String? = null,
        @Index var sessionId: String? = null,
        @Index var userId: String? = null,
        @Index var deviceId: String? = null,
        @Index var deviceIdentityKey: String? = null,
        var chainIndex: Int? = null
) : RealmObject() {

    companion object
}
