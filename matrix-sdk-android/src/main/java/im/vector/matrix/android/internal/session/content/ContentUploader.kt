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

package im.vector.matrix.android.internal.session.content

import arrow.core.Try
import arrow.core.Try.Companion.raise
import im.vector.matrix.android.api.auth.data.SessionParams
import im.vector.matrix.android.api.session.content.ContentAttachmentData
import im.vector.matrix.android.internal.di.MoshiProvider
import im.vector.matrix.android.internal.network.ProgressRequestBody
import okhttp3.HttpUrl
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import java.io.File
import java.io.IOException


internal class ContentUploader(private val okHttpClient: OkHttpClient,
                               private val sessionParams: SessionParams,
                               private val contentUploadProgressTracker: DefaultContentUploadStateTracker) {

    private val moshi = MoshiProvider.providesMoshi()
    private val responseAdapter = moshi.adapter(ContentUploadResponse::class.java)

    fun uploadFile(eventId: String, attachment: ContentAttachmentData): Try<ContentUploadResponse> {
        if (attachment.path == null || attachment.mimeType == null) {
            return raise(RuntimeException())
        }
        val file = File(attachment.path)
        val urlString = sessionParams.homeServerConnectionConfig.homeServerUri.toString() + URI_PREFIX_CONTENT_API + "upload"

        val urlBuilder = HttpUrl.parse(urlString)?.newBuilder()
                         ?: return raise(RuntimeException())

        val httpUrl = urlBuilder
                .addQueryParameter(
                        "filename", attachment.name
                ).build()

        val requestBody = RequestBody.create(
                MediaType.parse(attachment.mimeType),
                file
        )
        val progressRequestBody = ProgressRequestBody(requestBody, object : ProgressRequestBody.Listener {
            override fun onProgress(current: Long, total: Long) {
                contentUploadProgressTracker.setProgress(eventId, current, total)
            }
        })

        val request = Request.Builder()
                .url(httpUrl)
                .post(progressRequestBody)
                .build()

        val result = Try {
            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException()
                } else {
                    response.body()?.source()?.let {
                        responseAdapter.fromJson(it)
                    }
                    ?: throw IOException()
                }
            }
        }
        if (result.isFailure()) {
            contentUploadProgressTracker.setFailure(eventId)
        } else {
            contentUploadProgressTracker.setSuccess(eventId)
        }
        return result
    }
}