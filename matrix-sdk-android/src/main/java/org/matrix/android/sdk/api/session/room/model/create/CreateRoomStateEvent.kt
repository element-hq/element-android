/*
 * Copyright (c) 2021 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.api.session.room.model.create

import org.matrix.android.sdk.api.session.events.model.Content

data class CreateRoomStateEvent(
        /**
         * Required. The type of event to send.
         */
        val type: String,

        /**
         * Required. The content of the event.
         */
        val content: Content,

        /**
         * The state_key of the state event. Defaults to an empty string.
         */
        val stateKey: String = ""
)
