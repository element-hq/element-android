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

import io.realm.RealmList
import io.realm.RealmObject
import io.realm.annotations.RealmClass

/**
 * Keep all the editions of a message
 */
internal open class EditAggregatedSummaryEntity(
        // The list of the editions used to build the summary (might be out of sync if chunked received from message chunk)
        var editions: RealmList<EditionOfEvent> = RealmList()
) : RealmObject() {

    companion object
}

@RealmClass(embedded = true)
internal open class EditionOfEvent(
        var senderId: String = "",
        var eventId: String = "",
        var content: String? = null,
        var timestamp: Long = 0,
        var isLocalEcho: Boolean = false
) : RealmObject()
