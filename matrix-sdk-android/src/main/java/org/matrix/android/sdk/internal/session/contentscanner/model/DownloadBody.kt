/*
 * Copyright 2021 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.session.contentscanner.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.matrix.android.sdk.api.session.crypto.model.EncryptedFileInfo
import org.matrix.android.sdk.internal.di.MoshiProvider
import org.matrix.android.sdk.internal.util.JsonCanonicalizer

@JsonClass(generateAdapter = true)
internal data class DownloadBody(
        @Json(name = "file") val file: EncryptedFileInfo? = null,
        @Json(name = "encrypted_body") val encryptedBody: EncryptedBody? = null
)

@JsonClass(generateAdapter = true)
internal data class EncryptedBody(
        @Json(name = "ciphertext") val cipherText: String,
        @Json(name = "mac") val mac: String,
        @Json(name = "ephemeral") val ephemeral: String
)

internal fun DownloadBody.toJson(): String = MoshiProvider.providesMoshi().adapter(DownloadBody::class.java).toJson(this)

internal fun DownloadBody.toCanonicalJson() = JsonCanonicalizer.getCanonicalJson(DownloadBody::class.java, this)
