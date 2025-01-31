/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.di

import androidx.annotation.Nullable
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonQualifier
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import java.lang.reflect.Type

@Retention(AnnotationRetention.RUNTIME)
@JsonQualifier
internal annotation class SerializeNulls {
    companion object {
        val JSON_ADAPTER_FACTORY: JsonAdapter.Factory = object : JsonAdapter.Factory {
            @Nullable
            override fun create(type: Type, annotations: Set<Annotation>, moshi: Moshi): JsonAdapter<*>? {
                val nextAnnotations = Types.nextAnnotations(annotations, SerializeNulls::class.java)
                        ?: return null
                return moshi.nextAdapter<Any>(this, type, nextAnnotations).serializeNulls()
            }
        }
    }
}
