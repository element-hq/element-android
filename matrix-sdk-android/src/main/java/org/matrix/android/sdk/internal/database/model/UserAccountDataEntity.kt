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

package org.matrix.android.sdk.internal.database.model

import io.realm.RealmObject
import io.realm.annotations.Index

/**
 * Clients can store custom config data for their account on their homeserver.
 * This account data will be synced between different devices and can persist across installations on a particular device.
 * Users may only view the account data for their own account.
 * The account_data may be either global or scoped to a particular rooms.
 */
internal open class UserAccountDataEntity(
        @Index var type: String? = null,
        var contentStr: String? = null
) : RealmObject() {

    companion object
}
