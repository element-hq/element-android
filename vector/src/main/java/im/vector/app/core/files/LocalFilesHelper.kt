/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.core.files

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import org.matrix.android.sdk.api.extensions.orFalse
import java.io.InputStream
import javax.inject.Inject

class LocalFilesHelper @Inject constructor(private val context: Context) {
    fun isLocalFile(fileUri: String?): Boolean {
        return fileUri
                ?.let { Uri.parse(it) }
                ?.let { DocumentFile.fromSingleUri(context, it) }
                ?.exists()
                .orFalse()
    }

    fun openInputStream(fileUri: String?): InputStream? {
        return fileUri
                ?.takeIf { isLocalFile(it) }
                ?.let { Uri.parse(it) }
                ?.let { context.contentResolver.openInputStream(it) }
    }
}
