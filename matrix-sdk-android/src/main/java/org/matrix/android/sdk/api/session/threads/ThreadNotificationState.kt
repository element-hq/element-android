/*
 * Copyright 2021 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.api.session.threads

/**
 * This class defines the state of a thread notification
 */
enum class ThreadNotificationState {

    // There are no new message
    NO_NEW_MESSAGE,

    // There is at least one new message
    NEW_MESSAGE,

    // The is at least one new message that should be highlighted
    // ex. "Hello @aris.kotsomitopoulos"
    NEW_HIGHLIGHTED_MESSAGE;
}
