/*
 * Copyright 2021 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.api.session

import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.util.JsonDict

interface LiveEventListener {

    fun onLiveEvent(roomId: String, event: Event)

    fun onPaginatedEvent(roomId: String, event: Event)

    fun onEventDecrypted(event: Event, clearEvent: JsonDict)

    fun onEventDecryptionError(event: Event, throwable: Throwable)

    fun onLiveToDeviceEvent(event: Event)

    // Maybe later add more, like onJoin, onLeave..
}
