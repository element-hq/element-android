/*
 * Copyright 2019 New Vector Ltd
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

package org.matrix.android.sdk.internal.session.content

import android.content.Context
import android.net.Uri
import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.greenrobot.eventbus.EventBus
import org.matrix.android.sdk.api.session.content.ContentUrlResolver
import org.matrix.android.sdk.internal.di.Authenticated
import org.matrix.android.sdk.internal.network.ProgressRequestBody
import org.matrix.android.sdk.internal.network.awaitResponse
import org.matrix.android.sdk.internal.network.toFailure
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import javax.inject.Inject

internal class FileUploader @Inject constructor(@Authenticated
                                                private val okHttpClient: OkHttpClient,
                                                private val eventBus: EventBus,
                                                private val context: Context,
                                                contentUrlResolver: ContentUrlResolver,
                                                moshi: Moshi) {

    private val uploadUrl = contentUrlResolver.uploadUrl
    private val responseAdapter = moshi.adapter(ContentUploadResponse::class.java)

    suspend fun uploadFile(file: File,
                           filename: String?,
                           mimeType: String?,
                           progressListener: ProgressRequestBody.Listener? = null): ContentUploadResponse {
        val uploadBody = file.asRequestBody(mimeType?.toMediaTypeOrNull())
        return upload(uploadBody, filename, progressListener)
    }

    suspend fun uploadByteArray(byteArray: ByteArray,
                                filename: String?,
                                mimeType: String?,
                                progressListener: ProgressRequestBody.Listener? = null): ContentUploadResponse {
        val uploadBody = byteArray.toRequestBody(mimeType?.toMediaTypeOrNull())
        return upload(uploadBody, filename, progressListener)
    }

    suspend fun uploadFromUri(uri: Uri,
                              filename: String?,
                              mimeType: String?,
                              progressListener: ProgressRequestBody.Listener? = null): ContentUploadResponse {
        return withContext(Dispatchers.IO) {
            val inputStream = context.contentResolver.openInputStream(uri) ?: throw FileNotFoundException()

            inputStream.use {
                uploadByteArray(it.readBytes(), filename, mimeType, progressListener)
            }
        }
    }
    private suspend fun upload(uploadBody: RequestBody, filename: String?, progressListener: ProgressRequestBody.Listener?): ContentUploadResponse {
        val urlBuilder = uploadUrl.toHttpUrlOrNull()?.newBuilder() ?: throw RuntimeException()

        val httpUrl = urlBuilder
                .addQueryParameter("filename", filename)
                .build()

        val requestBody = if (progressListener != null) ProgressRequestBody(uploadBody, progressListener) else uploadBody

        val request = Request.Builder()
                .url(httpUrl)
                .post(requestBody)
                .build()

        return okHttpClient.newCall(request).awaitResponse().use { response ->
            if (!response.isSuccessful) {
                throw response.toFailure(eventBus)
            } else {
                response.body?.source()?.let {
                    responseAdapter.fromJson(it)
                }
                        ?: throw IOException()
            }
        }
    }
}
