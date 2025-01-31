/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.util

import java.net.URLEncoder

/**
 * Append param and value to a Url, using "?" or "&". Value parameter will be encoded
 * Return this for chaining purpose
 */
fun StringBuilder.appendParamToUrl(param: String, value: String): StringBuilder {
    if (contains("?")) {
        append("&")
    } else {
        append("?")
    }

    append(param)
    append("=")
    append(URLEncoder.encode(value, "utf-8"))

    return this
}

fun StringBuilder.appendParamsToUrl(params: Map<String, String>): StringBuilder {
    params.forEach { (param, value) ->
        appendParamToUrl(param, value)
    }
    return this
}
