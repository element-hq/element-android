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

package org.matrix.android.sdk.internal.crypto.keysbackup.algorithm

import org.matrix.android.sdk.api.session.crypto.keysbackup.MegolmBackupAuthData
import org.matrix.android.sdk.api.util.JsonDict
import org.matrix.android.sdk.internal.crypto.MegolmSessionData
import org.matrix.android.sdk.internal.crypto.keysbackup.model.rest.KeysBackupData

internal interface KeysBackupAlgorithm {
    val authData: MegolmBackupAuthData
    val untrusted: Boolean

    fun encryptSession(sessionData: MegolmSessionData): JsonDict?
    fun decryptSessions(recoveryKey: String, data: KeysBackupData): List<MegolmSessionData>
    fun keyMatches(key: String): Boolean
    fun release()
}
