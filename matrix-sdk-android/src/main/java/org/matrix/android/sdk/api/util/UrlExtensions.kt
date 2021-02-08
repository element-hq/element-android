/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
