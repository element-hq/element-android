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

package im.vector.lib.multipicker.utils

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal fun createTemporaryMediaFile(context: Context, mediaType: MediaType): File {
    val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val storageDir: File = File(context.filesDir, "media").also { it.mkdirs() }
    val fileSuffix = when (mediaType) {
        MediaType.IMAGE -> ".jpg"
        MediaType.VIDEO -> ".mp4"
    }

    return File.createTempFile(
            "${timeStamp}_",
            fileSuffix,
            storageDir
    )
}

internal enum class MediaType {
    IMAGE, VIDEO
}
