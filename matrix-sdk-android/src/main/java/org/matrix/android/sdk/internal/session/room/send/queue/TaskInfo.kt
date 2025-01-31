/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.room.send.queue

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import org.matrix.android.sdk.api.extensions.tryOrNull
import org.matrix.android.sdk.internal.di.SerializeNulls
import org.matrix.android.sdk.internal.network.parsing.RuntimeJsonAdapterFactory

/**
 * Info that need to be persisted by the sender thread.
 * With polymorphic moshi parsing.
 */
internal interface TaskInfo {
    val type: String
    val order: Int

    companion object {
        const val TYPE_UNKNOWN = "TYPE_UNKNOWN"
        const val TYPE_SEND = "TYPE_SEND"
        const val TYPE_REDACT = "TYPE_REDACT"

        private val moshi = Moshi.Builder()
                .add(
                        RuntimeJsonAdapterFactory.of(TaskInfo::class.java, "type", FallbackTaskInfo::class.java)
                                .registerSubtype(SendEventTaskInfo::class.java, TYPE_SEND)
                                .registerSubtype(RedactEventTaskInfo::class.java, TYPE_REDACT)
                )
                .add(SerializeNulls.JSON_ADAPTER_FACTORY)
                .build()

        fun map(info: TaskInfo): String {
            return moshi.adapter(TaskInfo::class.java).toJson(info)
        }

        fun map(string: String): TaskInfo? {
            return tryOrNull { moshi.adapter(TaskInfo::class.java).fromJson(string) }
        }
    }
}

@JsonClass(generateAdapter = true)
internal data class SendEventTaskInfo(
        @Json(name = "type") override val type: String = TaskInfo.TYPE_SEND,
        @Json(name = "localEchoId") val localEchoId: String,
        @Json(name = "encrypt") val encrypt: Boolean?,
        @Json(name = "order") override val order: Int
) : TaskInfo

@JsonClass(generateAdapter = true)
internal data class RedactEventTaskInfo(
        @Json(name = "type") override val type: String = TaskInfo.TYPE_REDACT,
        @Json(name = "redactionLocalEcho") val redactionLocalEcho: String?,
        @Json(name = "order") override val order: Int
) : TaskInfo

@JsonClass(generateAdapter = true)
internal data class FallbackTaskInfo(
        @Json(name = "type") override val type: String = TaskInfo.TYPE_UNKNOWN,
        @Json(name = "order") override val order: Int
) : TaskInfo
