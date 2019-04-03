/*
 *
 *  * Copyright 2019 New Vector Ltd
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package im.vector.matrix.android.internal.session.room.media

import arrow.core.Try
import im.vector.matrix.android.api.auth.data.SessionParams
import im.vector.matrix.android.internal.session.content.URI_PREFIX_CONTENT_API
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import java.io.File
import java.io.IOException


internal class MediaUploader(private val okHttpClient: OkHttpClient,
                             private val sessionParams: SessionParams) {

    fun uploadFile(attachment: MediaAttachment): Try<String> {
        if (attachment.path == null || attachment.mimeType == null) {
            return Try.raise(RuntimeException())
        }
        val urlString = sessionParams.homeServerConnectionConfig.homeServerUri.toString() + URI_PREFIX_CONTENT_API + "upload"
        val file = File(attachment.path)

        // create RequestBody instance from file
        val requestFile = RequestBody.create(
                MediaType.parse(attachment.mimeType),
                file
        )
        val request = Request.Builder()
                .url(urlString)
                .post(requestFile)
                .build()

        return okHttpClient.newCall(request).execute().use { response ->
            if (response.isSuccessful) {
                Try.raise(IOException(""))
            } else {
                Try.just(response.message())
            }
        }
    }
}