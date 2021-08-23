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
 *
 */

package org.matrix.android.sdk.api.session.profile

import android.net.Uri
import androidx.lifecycle.LiveData
import org.matrix.android.sdk.api.auth.UserInteractiveAuthInterceptor
import org.matrix.android.sdk.api.session.identity.ThreePid
import org.matrix.android.sdk.api.util.JsonDict
import org.matrix.android.sdk.api.util.Optional

/**
 * This interface defines methods to handling profile information. It's implemented at the session level.
 */
interface ProfileService {

    companion object Constants {
        const val DISPLAY_NAME_KEY = "displayname"
        const val AVATAR_URL_KEY = "avatar_url"
    }

    /**
     * Return the current display name for this user
     * @param userId the userId param to look for
     *
     */
    suspend fun getDisplayName(userId: String): Optional<String>

    /**
     * Update the display name for this user
     * @param userId the userId to update the display name of
     * @param newDisplayName the new display name of the user
     */
    suspend fun setDisplayName(userId: String, newDisplayName: String)

    /**
     * Update the avatar for this user
     * @param userId the userId to update the avatar of
     * @param newAvatarUri the new avatar uri of the user
     * @param fileName the fileName of selected image
     */
    suspend fun updateAvatar(userId: String, newAvatarUri: Uri, fileName: String)

    /**
     * Return the current avatarUrl for this user.
     * @param userId the userId param to look for
     *
     */
    suspend fun getAvatarUrl(userId: String): Optional<String>

    /**
     * Get the combined profile information for this user.
     * This may return keys which are not limited to displayname or avatar_url.
     * If server is configured as limit_profile_requests_to_users_who_share_rooms: true then response can be HTTP 403.
     * @param userId the userId param to look for
     *
     */
    suspend fun getProfile(userId: String): JsonDict

    /**
     * Get the current user 3Pids
     */
    fun getThreePids(): List<ThreePid>

    /**
     * Get the current user 3Pids Live
     * @param refreshData set to true to fetch data from the homeserver
     */
    fun getThreePidsLive(refreshData: Boolean): LiveData<List<ThreePid>>

    /**
     * Get the pending 3Pids, i.e. ThreePids that have requested a token, but not yet validated by the user.
     */
    fun getPendingThreePids(): List<ThreePid>

    /**
     * Get the pending 3Pids Live
     */
    fun getPendingThreePidsLive(): LiveData<List<ThreePid>>

    /**
     * Add a 3Pids. This is the first step to add a ThreePid to an account. Then the threePid will be added to the pending threePid list.
     */
    suspend fun addThreePid(threePid: ThreePid)

    /**
     * Validate a code received by text message
     */
    suspend fun submitSmsCode(threePid: ThreePid.Msisdn, code: String)

    /**
     * Finalize adding a 3Pids. Call this method once the user has validated that he owns the ThreePid
     */
    suspend fun finalizeAddingThreePid(threePid: ThreePid,
                                       userInteractiveAuthInterceptor: UserInteractiveAuthInterceptor)

    /**
     * Cancel adding a threepid. It will remove locally stored data about this ThreePid
     */
    suspend fun cancelAddingThreePid(threePid: ThreePid)

    /**
     * Remove a 3Pid from the Matrix account.
     */
    suspend fun deleteThreePid(threePid: ThreePid)
}
