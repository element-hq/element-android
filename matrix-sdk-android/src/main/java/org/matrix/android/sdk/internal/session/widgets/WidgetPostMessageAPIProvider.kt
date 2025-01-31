/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
