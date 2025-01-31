/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.widgets

import dagger.Lazy
import okhttp3.OkHttpClient
import org.matrix.android.sdk.internal.di.Unauthenticated
import org.matrix.android.sdk.internal.network.RetrofitFactory
import org.matrix.android.sdk.internal.session.SessionScope
import javax.inject.Inject

@SessionScope
internal class WidgetsAPIProvider @Inject constructor(
        @Unauthenticated private val okHttpClient: Lazy<OkHttpClient>,
        private val retrofitFactory: RetrofitFactory
) {

    // Map to keep one WidgetAPI instance by serverUrl
    private val widgetsAPIs = mutableMapOf<String, WidgetsAPI>()

    fun get(serverUrl: String): WidgetsAPI {
        return widgetsAPIs.getOrPut(serverUrl) {
            retrofitFactory.create(okHttpClient, serverUrl).create(WidgetsAPI::class.java)
        }
    }
}
