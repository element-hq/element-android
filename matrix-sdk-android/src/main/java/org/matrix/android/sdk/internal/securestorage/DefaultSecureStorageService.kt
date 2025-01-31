/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.securestorage

import org.matrix.android.sdk.api.securestorage.SecretStoringUtils
import org.matrix.android.sdk.api.securestorage.SecureStorageService
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject

internal class DefaultSecureStorageService @Inject constructor(private val secretStoringUtils: SecretStoringUtils) : SecureStorageService {

    override fun securelyStoreObject(any: Any, keyAlias: String, outputStream: OutputStream) {
        secretStoringUtils.securelyStoreObject(any, keyAlias, outputStream)
    }

    override fun <T> loadSecureSecret(inputStream: InputStream, keyAlias: String): T? {
        return secretStoringUtils.loadSecureSecret(inputStream, keyAlias)
    }
}
