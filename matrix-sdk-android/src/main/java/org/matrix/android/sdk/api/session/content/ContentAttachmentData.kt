/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session.content

import android.net.Uri
import android.os.Parcelable
import androidx.exifinterface.media.ExifInterface
import com.squareup.moshi.JsonClass
import kotlinx.parcelize.Parcelize
import org.matrix.android.sdk.api.util.MimeTypes.normalizeMimeType
import org.matrix.android.sdk.internal.di.MoshiProvider

@Parcelize
@JsonClass(generateAdapter = true)
data class ContentAttachmentData(
        val size: Long = 0,
        val duration: Long? = 0,
        val date: Long = 0,
        val height: Long? = 0,
        val width: Long? = 0,
        val exifOrientation: Int = ExifInterface.ORIENTATION_UNDEFINED,
        val name: String? = null,
        val queryUri: Uri,
        val mimeType: String?,
        val type: Type,
        val waveform: List<Int>? = null
) : Parcelable {

    @JsonClass(generateAdapter = false)
    enum class Type {
        FILE,
        IMAGE,
        AUDIO,
        VIDEO,
        VOICE_MESSAGE
    }

    fun getSafeMimeType() = mimeType?.normalizeMimeType()

    fun toJsonString(): String {
        return MoshiProvider.providesMoshi().adapter(ContentAttachmentData::class.java).toJson(this)
    }

    companion object {
        fun fromJsonString(json: String): ContentAttachmentData? {
            return MoshiProvider.providesMoshi().adapter(ContentAttachmentData::class.java).fromJson(json)
        }
    }
}
