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

package org.matrix.android.sdk.internal.network.interceptors

import androidx.annotation.NonNull
import org.matrix.android.sdk.BuildConfig
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import timber.log.Timber

class FormattedJsonHttpLogger : HttpLoggingInterceptor.Logger {

    companion object {
        private const val INDENT_SPACE = 2
    }

    /**
     * Log the message and try to log it again as a JSON formatted string
     * Note: it can consume a lot of memory but it is only in DEBUG mode
     *
     * @param message
     */
    @Synchronized
    override fun log(@NonNull message: String) {
        // In RELEASE there is no log, but for sure, test again BuildConfig.DEBUG
        if (BuildConfig.DEBUG) {
            Timber.v(message)

            if (message.startsWith("{")) {
                // JSON Detected
                try {
                    val o = JSONObject(message)
                    logJson(o.toString(INDENT_SPACE))
                } catch (e: JSONException) {
                    // Finally this is not a JSON string...
                    Timber.e(e)
                }
            } else if (message.startsWith("[")) {
                // JSON Array detected
                try {
                    val o = JSONArray(message)
                    logJson(o.toString(INDENT_SPACE))
                } catch (e: JSONException) {
                    // Finally not JSON...
                    Timber.e(e)
                }
            }
            // Else not a json string to log
        }
    }

    private fun logJson(formattedJson: String) {
        val arr = formattedJson.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        for (s in arr) {
            Timber.v(s)
        }
    }
}
