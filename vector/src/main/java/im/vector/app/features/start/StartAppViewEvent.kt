/*
 * Copyright (c) 2022 New Vector Ltd
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

package im.vector.app.features.start

import im.vector.app.core.platform.VectorViewEvents

sealed interface StartAppViewEvent : VectorViewEvents {
    /**
     * Will be sent if the process is taking more than 1 second.
     */
    object StartForegroundService : StartAppViewEvent

    /**
     * Will be sent when the current Session has been set.
     */
    object AppStarted : StartAppViewEvent
}
