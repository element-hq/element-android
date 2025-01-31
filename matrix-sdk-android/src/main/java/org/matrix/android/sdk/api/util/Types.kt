/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.util

import com.squareup.moshi.Types
import java.lang.reflect.ParameterizedType

typealias JsonDict = Map<String, @JvmSuppressWildcards Any>

val emptyJsonDict = emptyMap<String, Any>()

internal val JSON_DICT_PARAMETERIZED_TYPE: ParameterizedType = Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
