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

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import timber.log.Timber
import java.util.*

/**
 * Build canonical Json
 * Doc: https://matrix.org/docs/spec/appendices.html#canonical-json
 */
object JsonCanonicalizer {

    fun canonicalize(json: String): String {
        var can: String? = null
        try {
            val _json = JSONObject(json)

            can = _canonicalize(_json)
        } catch (e: JSONException) {
            Timber.e(e, "Unable to canonicalize")
        }

        if (can == null) {
            Timber.e("Error")
            return json
        }

        return can
    }

    /**
     * Canonicalize a JsonElement element
     *
     * @param src the src
     * @return the canonicalize element
     */
    private fun _canonicalize(src: Any?): String? {
        // sanity check
        if (null == src) {
            return null
        }

        when (src) {
            is JSONArray  -> {
                // Canonicalize each element of the array
                val srcArray = src as JSONArray?
                val result = StringBuilder("[")

                for (i in 0 until srcArray!!.length()) {
                    result.append(_canonicalize(srcArray.get(i)))
                    if (i < srcArray.length() - 1) {
                        result.append(",")
                    }
                }

                result.append("]")

                return result.toString()
            }
            is JSONObject -> {
                // Sort the attributes by name, and the canonicalize each element of the object
                val result = StringBuilder("{")

                val attributes = TreeSet<String>()
                for (entry in src.keys()) {
                    attributes.add(entry)
                }

                for (attribute in attributes.withIndex()) {
                    result.append("\"")
                            .append(attribute.value)
                            .append("\"")
                            .append(":")
                            .append(_canonicalize(src[attribute.value]))

                    if (attribute.index < attributes.size - 1) {
                        result.append(",")
                    }
                }

                result.append("}")

                return result.toString()
            }
            is String     -> return  JSONObject.quote(src.toString())
            else          -> return src.toString()
        }
    }

}