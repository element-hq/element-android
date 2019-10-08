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

import im.vector.riotx.core.resources.MIME_TYPE_ALL_CONTENT

data class Attachment(val path: String,
                      val mimeType: String,
                      val name: String? = "",
                      val width: Long? = 0,
                      val height: Long? = 0,
                      val size: Long = 0,
                      val duration: Long? = 0,
                      val date: Long = 0) {

    val type: Int
        get() {
            if (mimeType == null) {
                return TYPE_FILE
            }
            return when {
                mimeType.startsWith("image/") -> TYPE_IMAGE
                mimeType.startsWith("video/") -> TYPE_VIDEO
                mimeType.startsWith("audio/")
                                              -> TYPE_AUDIO
                else                          -> TYPE_FILE
            }
        }

    companion object {
        val TYPE_FILE = 0
        val TYPE_IMAGE = 1
        val TYPE_AUDIO = 2
        val TYPE_VIDEO = 3
    }
}