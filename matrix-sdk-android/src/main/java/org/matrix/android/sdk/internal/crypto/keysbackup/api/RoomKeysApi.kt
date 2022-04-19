/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.crypto.keysbackup.api

import org.matrix.android.sdk.api.session.crypto.keysbackup.KeysVersion
import org.matrix.android.sdk.api.session.crypto.keysbackup.KeysVersionResult
import org.matrix.android.sdk.internal.crypto.keysbackup.model.rest.BackupKeysResult
import org.matrix.android.sdk.internal.crypto.keysbackup.model.rest.CreateKeysBackupVersionBody
import org.matrix.android.sdk.internal.crypto.keysbackup.model.rest.KeyBackupData
import org.matrix.android.sdk.internal.crypto.keysbackup.model.rest.KeysBackupData
import org.matrix.android.sdk.internal.crypto.keysbackup.model.rest.RoomKeysBackupData
import org.matrix.android.sdk.internal.crypto.keysbackup.model.rest.UpdateKeysBackupVersionBody
import org.matrix.android.sdk.internal.network.NetworkConstants
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Ref: https://matrix.org/docs/spec/client_server/unstable#server-side-key-backups
 */
internal interface RoomKeysApi {

    /* ==========================================================================================
     * Backup versions management
     * ========================================================================================== */

    /**
     * Create a new keys backup version.
     * @param createKeysBackupVersionBody the body
     */
    @POST(NetworkConstants.URI_API_PREFIX_PATH_UNSTABLE + "room_keys/version")
    suspend fun createKeysBackupVersion(@Body createKeysBackupVersionBody: CreateKeysBackupVersionBody): KeysVersion

    /**
     * Get the key backup last version
     * If not supported by the server, an error is returned: {"errcode":"M_NOT_FOUND","error":"No backup found"}
     */
    @GET(NetworkConstants.URI_API_PREFIX_PATH_UNSTABLE + "room_keys/version")
    suspend fun getKeysBackupLastVersion(): KeysVersionResult

    /**
     * Get information about the given version.
     * If not supported by the server, an error is returned: {"errcode":"M_NOT_FOUND","error":"No backup found"}
     *
     * @param version  version
     */
    @GET(NetworkConstants.URI_API_PREFIX_PATH_UNSTABLE + "room_keys/version/{version}")
    suspend fun getKeysBackupVersion(@Path("version") version: String): KeysVersionResult

    /**
     * Update information about the given version.
     * @param version                     version
     * @param updateKeysBackupVersionBody the body
     */
    @PUT(NetworkConstants.URI_API_PREFIX_PATH_UNSTABLE + "room_keys/version/{version}")
    suspend fun updateKeysBackupVersion(@Path("version") version: String,
                                        @Body keysBackupVersionBody: UpdateKeysBackupVersionBody)

    /* ==========================================================================================
     * Storing keys
     * ========================================================================================== */

    /**
     * Store the key for the given session in the given room, using the given backup version.
     *
     *
     * If the server already has a backup in the backup version for the given session and room, then it will
     * keep the "better" one. To determine which one is "better", key backups are compared first by the is_verified
     * flag (true is better than false), then by the first_message_index (a lower number is better), and finally by
     * forwarded_count (a lower number is better).
     *
     * @param roomId        the room id
     * @param sessionId     the session id
     * @param version       the version of the backup
     * @param keyBackupData the data to send
     */
    @PUT(NetworkConstants.URI_API_PREFIX_PATH_UNSTABLE + "room_keys/keys/{roomId}/{sessionId}")
    suspend fun storeRoomSessionData(@Path("roomId") roomId: String,
                                     @Path("sessionId") sessionId: String,
                                     @Query("version") version: String,
                                     @Body keyBackupData: KeyBackupData): BackupKeysResult

    /**
     * Store several keys for the given room, using the given backup version.
     *
     * @param roomId             the room id
     * @param version            the version of the backup
     * @param roomKeysBackupData the data to send
     */
    @PUT(NetworkConstants.URI_API_PREFIX_PATH_UNSTABLE + "room_keys/keys/{roomId}")
    suspend fun storeRoomSessionsData(@Path("roomId") roomId: String,
                                      @Query("version") version: String,
                                      @Body roomKeysBackupData: RoomKeysBackupData): BackupKeysResult

    /**
     * Store several keys, using the given backup version.
     *
     * @param version        the version of the backup
     * @param keysBackupData the data to send
     */
    @PUT(NetworkConstants.URI_API_PREFIX_PATH_UNSTABLE + "room_keys/keys")
    suspend fun storeSessionsData(@Query("version") version: String,
                                  @Body keysBackupData: KeysBackupData): BackupKeysResult

    /* ==========================================================================================
     * Retrieving keys
     * ========================================================================================== */

    /**
     * Retrieve the key for the given session in the given room from the backup.
     *
     * @param roomId    the room id
     * @param sessionId the session id
     * @param version   the version of the backup, or empty String to retrieve the last version
     */
    @GET(NetworkConstants.URI_API_PREFIX_PATH_UNSTABLE + "room_keys/keys/{roomId}/{sessionId}")
    suspend fun getRoomSessionData(@Path("roomId") roomId: String,
                                   @Path("sessionId") sessionId: String,
                                   @Query("version") version: String): KeyBackupData

    /**
     * Retrieve all the keys for the given room from the backup.
     *
     * @param roomId   the room id
     * @param version  the version of the backup, or empty String to retrieve the last version
     */
    @GET(NetworkConstants.URI_API_PREFIX_PATH_UNSTABLE + "room_keys/keys/{roomId}")
    suspend fun getRoomSessionsData(@Path("roomId") roomId: String,
                                    @Query("version") version: String): RoomKeysBackupData

    /**
     * Retrieve all the keys from the backup.
     *
     * @param version  the version of the backup, or empty String to retrieve the last version
     */
    @GET(NetworkConstants.URI_API_PREFIX_PATH_UNSTABLE + "room_keys/keys")
    suspend fun getSessionsData(@Query("version") version: String): KeysBackupData

    /* ==========================================================================================
     * Deleting keys
     * ========================================================================================== */

    /**
     * Deletes keys from the backup.
     */
    @DELETE(NetworkConstants.URI_API_PREFIX_PATH_UNSTABLE + "room_keys/keys/{roomId}/{sessionId}")
    suspend fun deleteRoomSessionData(@Path("roomId") roomId: String,
                                      @Path("sessionId") sessionId: String,
                                      @Query("version") version: String)

    /**
     * Deletes keys from the backup.
     */
    @DELETE(NetworkConstants.URI_API_PREFIX_PATH_UNSTABLE + "room_keys/keys/{roomId}")
    suspend fun deleteRoomSessionsData(@Path("roomId") roomId: String,
                                       @Query("version") version: String)

    /**
     * Deletes keys from the backup.
     */
    @DELETE(NetworkConstants.URI_API_PREFIX_PATH_UNSTABLE + "room_keys/keys")
    suspend fun deleteSessionsData(@Query("version") version: String)

    /* ==========================================================================================
     * Deleting backup
     * ========================================================================================== */

    /**
     * Deletes a backup.
     */
    @DELETE(NetworkConstants.URI_API_PREFIX_PATH_UNSTABLE + "room_keys/version/{version}")
    suspend fun deleteBackup(@Path("version") version: String)
}
