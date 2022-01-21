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

package org.matrix.android.sdk.api.auth.refresh

import org.matrix.android.sdk.api.auth.data.RefreshResult

/**
 * Set of methods to be able to refresh authentication for an account on a homeserver.
 *
 * More documentation can be found in the file https://github.com/vector-im/element-android/blob/main/docs/signin.md
 */
interface RefreshWizard {
    /**
     * Refresh out access token.
     *
     * @param refreshToken the refresh token to be used to get a new access token. They rotate so should be used only once, the result contains the next one.
     * @return a [RefreshResult] if the refresh is successful. Contains the new access token, the refresh token and any expiry on the access token.
     */
    suspend fun refresh(refreshToken: String): RefreshResult
}
