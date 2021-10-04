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

package org.matrix.android.sdk.internal.session.widgets

import android.content.Context
import timber.log.Timber
import javax.inject.Inject

internal class WidgetPostMessageAPIProvider @Inject constructor(private val context: Context) {

    private var postMessageAPIString: String? = null

    fun get(): String? {
        if (postMessageAPIString == null) {
            postMessageAPIString = readFromAsset(context)
        }
        return postMessageAPIString
    }

    private fun readFromAsset(context: Context): String? {
        return try {
            context.assets.open("postMessageAPI.js").bufferedReader().use {
                it.readText()
            }
        } catch (failure: Throwable) {
            Timber.e(failure, "Reading postMessageAPI.js asset failed")
            null
        }
    }
}
