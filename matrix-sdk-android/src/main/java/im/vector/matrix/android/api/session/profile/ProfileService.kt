/*
 * Copyright 2020 New Vector Ltd
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
 *
 */

package im.vector.matrix.android.api.session.profile

import androidx.lifecycle.LiveData
import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.api.session.identity.ThreePid
import im.vector.matrix.android.api.util.Cancelable
import im.vector.matrix.android.api.util.JsonDict
import im.vector.matrix.android.api.util.Optional

/**
 * This interface defines methods to handling profile information. It's implemented at the session level.
 */
interface ProfileService {

    companion object Constants {
        const val DISPLAY_NAME_KEY = "displayname"
        const val AVATAR_URL_KEY = "avatar_url"
    }

    /**
     * Return the current dispayname for this user
     * @param userId the userId param to look for
     *
     */
    fun getDisplayName(userId: String, matrixCallback: MatrixCallback<Optional<String>>): Cancelable

    /**
     * Return the current avatarUrl for this user.
     * @param userId the userId param to look for
     *
     */
    fun getAvatarUrl(userId: String, matrixCallback: MatrixCallback<Optional<String>>): Cancelable

    /**
     * Get the combined profile information for this user.
     * This may return keys which are not limited to displayname or avatar_url.
     * @param userId the userId param to look for
     *
     */
    fun getProfile(userId: String, matrixCallback: MatrixCallback<JsonDict>): Cancelable

    /**
     * Get the current user 3Pids
     */
    fun getThreePids(): List<ThreePid>

    /**
     * Get the current user 3Pids Live
     */
    fun getThreePidsLive(): LiveData<List<ThreePid>>
}
