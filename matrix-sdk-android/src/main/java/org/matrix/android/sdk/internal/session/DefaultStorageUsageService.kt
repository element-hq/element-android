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

package org.matrix.android.sdk.internal.session

import org.matrix.android.sdk.api.extensions.tryOrNull
import org.matrix.android.sdk.api.session.StorageUsageService
import org.matrix.android.sdk.internal.di.CacheDirectory
import org.matrix.android.sdk.internal.di.SessionFilesDirectory
import java.io.File
import javax.inject.Inject

class DefaultStorageUsageService @Inject constructor(
        @SessionFilesDirectory val directory: File,
        @CacheDirectory val cacheFile: File
) : StorageUsageService {

    // "disk_store.realm"
    // "crypto_store.realm"

    override fun sessionDataBaseSize(): Long {
        return tryOrNull { File(directory, "disk_store.realm").length() } ?: 0
    }

    override fun cryptoDataBaseSize(): Long {
        return tryOrNull { File(directory, "crypto_store.realm").length() } ?: 0
    }

    override fun cacheDirectorySize(folderName: String): Long {
        return tryOrNull {
            File(cacheFile, folderName)
                    .walkTopDown()
                    .onEnter {
                        true
                    }
                    .sumOf { it.length() }
        } ?: 0L
    }
}
