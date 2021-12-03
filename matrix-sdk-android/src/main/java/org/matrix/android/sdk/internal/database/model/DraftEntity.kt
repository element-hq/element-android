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

package org.matrix.android.sdk.internal.database.model

import io.realm.RealmObject

internal open class DraftEntity(var content: String = "",
                                var draftMode: String = MODE_REGULAR,
                                var linkedEventId: String = ""
) : RealmObject() {

    companion object {
        const val MODE_REGULAR = "REGULAR"
        const val MODE_EDIT = "EDIT"
        const val MODE_REPLY = "REPLY"
        const val MODE_QUOTE = "QUOTE"
        const val MODE_VOICE = "VOICE"
    }
}
