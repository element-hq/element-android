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
