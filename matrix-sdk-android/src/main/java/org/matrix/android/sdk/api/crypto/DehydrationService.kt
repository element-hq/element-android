/*
 * Copyright (c) 2021 New Vector Ltd
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

package org.matrix.android.sdk.api.crypto

import org.matrix.android.sdk.api.auth.data.Credentials
import org.matrix.android.sdk.api.auth.data.HomeServerConnectionConfig
import org.matrix.android.sdk.api.session.crypto.CryptoService
import org.matrix.android.sdk.internal.crypto.dehydration.DehydrationResult
import org.matrix.android.sdk.internal.crypto.dehydration.RehydrationResult

/**
 * This interface defines methods to rehydrate and dehydrate device.
 */
interface DehydrationService {

    /**
     * Gets dehydrated device, unpickles, claims and exports it to use as current device.
     */
    suspend fun rehydrateDevice(
            credentials: Credentials,
            homeServerConnectionConfig: HomeServerConnectionConfig,
            dehydrationKey: String): RehydrationResult

    /**
     * Creates a new dehydrated device. Uploading a new dehydrated device will remove any previously-set dehydrated device.
     */
    suspend fun dehydrateDevice(
            credentials: Credentials,
            homeServerConnectionConfig: HomeServerConnectionConfig,
            deviceDisplayName: String,
            dehydrationKey: ByteArray,
            cryptoService: CryptoService): DehydrationResult
}
