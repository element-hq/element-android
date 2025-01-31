/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.network

import okhttp3.ResponseBody
import retrofit2.Converter
import retrofit2.Retrofit
import java.lang.reflect.Type

internal object UnitConverterFactory : Converter.Factory() {
    override fun responseBodyConverter(
            type: Type, annotations: Array<out Annotation>,
            retrofit: Retrofit
    ): Converter<ResponseBody, *>? {
        return if (type == Unit::class.java) UnitConverter else null
    }

    private object UnitConverter : Converter<ResponseBody, Unit> {
        override fun convert(value: ResponseBody) {
            value.close()
        }
    }
}
