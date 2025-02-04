/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.crypto.keys

import android.content.Context
import android.net.Uri
import im.vector.app.core.intent.getMimeTypeFromUri
import im.vector.app.core.resources.openResource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.crypto.model.ImportRoomKeysResult
import javax.inject.Inject

class KeysImporter @Inject constructor(
        private val context: Context,
        private val session: Session
) {
    /**
     * Import keys from provided Uri.
     */
    suspend fun import(
            uri: Uri,
            mimetype: String?,
            password: String
    ): ImportRoomKeysResult {
        return withContext(Dispatchers.IO) {
            val resource = openResource(context, uri, mimetype ?: getMimeTypeFromUri(context, uri))
            val stream = resource?.mContentStream ?: throw Exception("Error")
            val data = stream.use { it.readBytes() }
            session.cryptoService().importRoomKeys(data, password, null)
        }
    }
}
