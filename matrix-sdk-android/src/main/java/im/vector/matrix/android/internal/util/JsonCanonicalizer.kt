/*
 * Copyright 2019 New Vector Ltd
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

package im.vector.matrix.android.internal.util

import androidx.annotation.VisibleForTesting
import im.vector.matrix.android.internal.di.MoshiProvider
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import timber.log.Timber
import java.util.TreeSet

/**
 * Build canonical Json
 * Doc: https://matrix.org/docs/spec/appendices.html#canonical-json
 */
object JsonCanonicalizer {

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
                val result = StringBuilder("[")

                for (i in 0 until any.length()) {
                    result.append(canonicalizeRecursive(any.get(i)))
                    if (i < any.length() - 1) {
                        result.append(",")
                    }
                }

                result.append("]")

                return result.toString()
            }
            is JSONObject -> {
                // Sort the attributes by name, and the canonicalize each element of the JSONObject
                val result = StringBuilder("{")

                val attributes = TreeSet<String>()
                for (entry in any.keys()) {
                    attributes.add(entry)
                }

                for (attribute in attributes.withIndex()) {
                    result.append("\"")
                            .append(attribute.value)
                            .append("\"")
                            .append(":")
                            .append(canonicalizeRecursive(any[attribute.value]))

                    if (attribute.index < attributes.size - 1) {
                        result.append(",")
                    }
                }

                result.append("}")

                return result.toString()
            }
            is String     -> return JSONObject.quote(any)
            else          -> return any.toString()
        }
    }

}