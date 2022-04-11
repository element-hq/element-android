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

package org.matrix.android.sdk.internal.util

import androidx.annotation.VisibleForTesting
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.matrix.android.sdk.internal.di.MoshiProvider
import timber.log.Timber
import java.util.TreeSet

/**
 * Build canonical Json
 * Doc: https://matrix.org/docs/spec/appendices.html#canonical-json
 */
internal object JsonCanonicalizer {

    fun <T> getCanonicalJson(type: Class<T>, o: T): String {
        val adapter = MoshiProvider.providesMoshi().adapter<T>(type)

        // Canonicalize manually
        return canonicalize(adapter.toJson(o))
                .replace("\\/", "/")
    }

    @VisibleForTesting
    fun canonicalize(jsonString: String): String {
        return try {
            val jsonObject = JSONObject(jsonString)

            canonicalizeRecursive(jsonObject)
        } catch (e: JSONException) {
            Timber.e(e, "Unable to canonicalize")
            jsonString
        }
    }

    /**
     * Canonicalize a JSON element
     *
     * @param src the src
     * @return the canonicalize element
     */
    private fun canonicalizeRecursive(any: Any): String {
        when (any) {
            is JSONArray  -> {
                // Canonicalize each element of the array
                return (0 until any.length()).joinToString(separator = ",", prefix = "[", postfix = "]") {
                    canonicalizeRecursive(any.get(it))
                }
            }
            is JSONObject -> {
                // Sort the attributes by name, and the canonicalize each element of the JSONObject

                val attributes = TreeSet<String>()
                for (entry in any.keys()) {
                    attributes.add(entry)
                }

                return buildString {
                    append("{")
                    for ((index, value) in attributes.withIndex()) {
                        append("\"")
                        append(value)
                        append("\"")
                        append(":")
                        append(canonicalizeRecursive(any[value]))

                        if (index < attributes.size - 1) {
                            append(",")
                        }
                    }
                    append("}")
                }
            }
            is String     -> return JSONObject.quote(any)
            else          -> return any.toString()
        }
    }
}
