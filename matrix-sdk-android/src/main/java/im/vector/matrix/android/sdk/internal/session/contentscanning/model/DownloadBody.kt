/*
 * Copyright 2020 New Vector Ltd - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package im.vector.matrix.android.sdk.internal.session.contentscanning.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.matrix.android.sdk.internal.crypto.model.rest.EncryptedFileInfo
import org.matrix.android.sdk.internal.di.MoshiProvider
import org.matrix.android.sdk.internal.util.JsonCanonicalizer

@JsonClass(generateAdapter = true)
data class DownloadBody(
        @Json(name = "file") val file: EncryptedFileInfo? = null,
        @Json(name = "encrypted_body") val encryptedBody: EncryptedBody? = null
)

@JsonClass(generateAdapter = true)
data class EncryptedBody(
        @Json(name = "ciphertext") val cipherText: String,
        @Json(name = "mac") val mac: String,
        @Json(name = "ephemeral") val ephemeral: String
)

fun DownloadBody.toJson(): String = MoshiProvider.providesMoshi().adapter(DownloadBody::class.java).toJson(this)

fun DownloadBody.toCanonicalJson() = JsonCanonicalizer.getCanonicalJson(DownloadBody::class.java, this)
