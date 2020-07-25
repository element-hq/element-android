/*
 * Copyright 2019 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.riotx.features.attachments

import im.vector.matrix.android.api.session.content.ContentAttachmentData
import im.vector.riotx.multipicker.entity.MultiPickerAudioType
import im.vector.riotx.multipicker.entity.MultiPickerBaseType
import im.vector.riotx.multipicker.entity.MultiPickerContactType
import im.vector.riotx.multipicker.entity.MultiPickerFileType
import im.vector.riotx.multipicker.entity.MultiPickerImageType
import im.vector.riotx.multipicker.entity.MultiPickerVideoType

fun MultiPickerContactType.toContactAttachment(): ContactAttachment {
    return ContactAttachment(
            displayName = displayName,
            photoUri = photoUri,
            emails = emailList.toList(),
            phones = phoneNumberList.toList()
    )
}

fun toContentAttachmentData(data : MultiPickerBaseType) = when (data) {
    is MultiPickerAudioType -> ContentAttachmentData(
            mimeType = data.mimeType,
            type = ContentAttachmentData.Type.AUDIO,
            size = data.size,
            name = data.displayName,
            duration = data.duration,
            queryUri = data.contentUri
    )
    is MultiPickerFileType  -> ContentAttachmentData(
            mimeType = data.mimeType,
            type = ContentAttachmentData.Type.FILE,
            size = data.size,
            name = data.displayName,
            queryUri = data.contentUri
    )
    is MultiPickerImageType -> ContentAttachmentData(
            mimeType = data.mimeType,
            type = ContentAttachmentData.Type.IMAGE,
            name = data.displayName,
            size = data.size,
            height = data.height.toLong(),
            width = data.width.toLong(),
            exifOrientation = data.orientation,
            queryUri = data.contentUri
    )
    is MultiPickerVideoType -> ContentAttachmentData(
            mimeType = data.mimeType,
            type = ContentAttachmentData.Type.VIDEO,
            size = data.size,
            height = data.height.toLong(),
            width = data.width.toLong(),
            duration = data.duration,
            name = data.displayName,
            queryUri = data.contentUri
    )
}
