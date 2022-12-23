/*
 * Copyright 2022 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.api.session.permalinks

/**
 * Service to handle deferred links, e.g. when user open link to the room but the app is not installed yet.
 */
interface DeferredPermalinkService {

    /**
     * Checks system clipboard for matrix.to links and returns first room link if any found.
     * @return first room link in clipboard or null if none is found
     */
    fun getLinkFromClipBoard(): String?
}
