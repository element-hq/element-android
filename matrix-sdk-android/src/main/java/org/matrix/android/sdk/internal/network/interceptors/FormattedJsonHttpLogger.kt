/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.network.interceptors

import androidx.annotation.NonNull
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import timber.log.Timber

internal class FormattedJsonHttpLogger(
        private val level: HttpLoggingInterceptor.Level
) : HttpLoggingInterceptor.Logger {

    companion object {
        private const val INDENT_SPACE = 2
    }

    /**
     * Log the message and try to log it again as a JSON formatted string.
     * Note: it can consume a lot of memory but it is only in DEBUG mode.
     *
     * @param message
     */
    @Synchronized
    override fun log(@NonNull message: String) {
        Timber.v(message)

        // Try to log formatted Json only if there is a chance that [message] contains Json.
        // It can be only the case if we log the bodies of Http requests.
        if (level != HttpLoggingInterceptor.Level.BODY) return

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

    private fun logJson(formattedJson: String) {
        formattedJson
                .lines()
                .dropLastWhile { it.isEmpty() }
                .forEach { Timber.v(it) }
    }
}
