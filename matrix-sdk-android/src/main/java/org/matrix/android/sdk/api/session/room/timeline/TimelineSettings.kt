/*
 * Copyright 2019 New Vector Ltd
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

package org.matrix.android.sdk.api.session.room.timeline

/**
 * Data class holding setting values for a [Timeline] instance.
 */
data class TimelineSettings(
        /**
         * The initial number of events to retrieve from cache. You might get less events if you don't have loaded enough yet.
         */
        val initialSize: Int,
        /**
         * A flag to filter edit events
         */
        val filterEdits: Boolean = false,
        /**
         * A flag to filter redacted events
         */
        val filterRedacted: Boolean = false,
        /**
         * A flag to filter useless events, such as membership events without any change
         */
        val filterUseless: Boolean = false,
        /**
         * A flag to filter by types. It should be used with [allowedTypes] field
         */
        val filterTypes: Boolean = false,
        /**
         * If [filterTypes] is true, the list of types allowed by the list.
         */
        val allowedTypes: List<String> = emptyList(),
        /**
         * If true, will build read receipts for each event.
         */
        val buildReadReceipts: Boolean = true
)
