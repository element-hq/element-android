/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.api.session.room.model.message

import android.webkit.MimeTypeMap
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.matrix.android.sdk.api.session.crypto.model.EncryptedFileInfo
import org.matrix.android.sdk.api.session.events.model.Content
import org.matrix.android.sdk.api.session.room.model.relation.RelationDefaultContent

@JsonClass(generateAdapter = true)
data class MessageFileContent(
        /**
         * Required. Must be 'm.file'.
         */
        @Json(name = MessageContent.MSG_TYPE_JSON_KEY) override val msgType: String,

        /**
         * Required. A human-readable description of the file. This is recommended to be the filename of the original upload.
         */
        @Json(name = "body") override val body: String,

        /**
         * The original filename of the uploaded file.
         */
        @Json(name = "filename") val filename: String? = null,

        /**
         * Information about the file referred to in url.
         */
        @Json(name = "info") val info: FileInfo? = null,

        /**
         * Required if the file is unencrypted. The URL (typically MXC URI) to the file.
         */
        @Json(name = "url") override val url: String? = null,

        @Json(name = "m.relates_to") override val relatesTo: RelationDefaultContent? = null,
        @Json(name = "m.new_content") override val newContent: Content? = null,

        /**
         * Required if the file is encrypted. Information on the encrypted file, as specified in End-to-end encryption.
         */
        @Json(name = "file") override val encryptedFileInfo: EncryptedFileInfo? = null
) : MessageWithAttachmentContent {

    override val mimeType: String?
        get() = info?.mimeType
                ?: MimeTypeMap.getFileExtensionFromUrl(filename ?: body)?.let { extension ->
                    MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
                }

    fun getFileName(): String {
        return filename ?: body
    }
}
