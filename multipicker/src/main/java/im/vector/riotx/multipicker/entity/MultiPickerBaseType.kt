/*
 * Copyright (c) 2020 New Vector Ltd
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

package im.vector.riotx.multipicker.entity

import android.net.Uri

sealed class MultiPickerBaseType {
    abstract val displayName: String?
    abstract val size: Long
    abstract val mimeType: String?
    abstract val contentUri: Uri
}

data class MultiPickerAudioType(
        override val displayName: String?,
        override val size: Long,
        override val mimeType: String?,
        override val contentUri: Uri,
        val duration: Long
) : MultiPickerBaseType()

data class MultiPickerFileType(
        override val displayName: String?,
        override val size: Long,
        override val mimeType: String?,
        override val contentUri: Uri
) : MultiPickerBaseType()

data class MultiPickerImageType(
        override val displayName: String?,
        override val size: Long,
        override val mimeType: String?,
        override val contentUri: Uri,
        val width: Int,
        val height: Int,
        val orientation: Int
) : MultiPickerBaseType()

data class MultiPickerVideoType(
        override val displayName: String?,
        override val size: Long,
        override val mimeType: String?,
        override val contentUri: Uri,
        val width: Int,
        val height: Int,
        val orientation: Int,
        val duration: Long
) : MultiPickerBaseType()
