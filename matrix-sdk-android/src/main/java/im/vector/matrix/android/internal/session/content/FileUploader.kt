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
import im.vector.matrix.android.internal.di.MoshiProvider
import im.vector.matrix.android.internal.network.ProgressRequestBody
import im.vector.matrix.android.internal.session.SessionScope
import okhttp3.*
import java.io.File
import java.io.IOException
import javax.inject.Inject


@SessionScope
internal class FileUploader @Inject constructor(private val okHttpClient: OkHttpClient,
                            sessionParams: SessionParams) {

    private val uploadUrl = DefaultContentUrlResolver.getUploadUrl(sessionParams.homeServerConnectionConfig)

    private val moshi = MoshiProvider.providesMoshi()
    private val responseAdapter = moshi.adapter(ContentUploadResponse::class.java)


    fun uploadFile(file: File,
                   filename: String?,
                   mimeType: String,
                   progressListener: ProgressRequestBody.Listener? = null): Try<ContentUploadResponse> {

        val uploadBody = RequestBody.create(MediaType.parse(mimeType), file)
        return upload(uploadBody, filename, progressListener)

    }

    fun uploadByteArray(byteArray: ByteArray,
                        filename: String?,
                        mimeType: String,
                        progressListener: ProgressRequestBody.Listener? = null): Try<ContentUploadResponse> {

        val uploadBody = RequestBody.create(MediaType.parse(mimeType), byteArray)
        return upload(uploadBody, filename, progressListener)

    }


    private fun upload(uploadBody: RequestBody, filename: String?, progressListener: ProgressRequestBody.Listener?): Try<ContentUploadResponse> {
        val urlBuilder = HttpUrl.parse(uploadUrl)?.newBuilder() ?: return raise(RuntimeException())

        val httpUrl = urlBuilder
                .addQueryParameter("filename", filename)
                .build()

        val requestBody = if (progressListener != null) ProgressRequestBody(uploadBody, progressListener) else uploadBody

        val request = Request.Builder()
                .url(httpUrl)
                .post(requestBody)
                .build()

        return Try {
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

    }

}