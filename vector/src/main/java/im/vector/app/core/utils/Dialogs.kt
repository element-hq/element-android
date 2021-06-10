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

package im.vector.app.core.utils

import android.content.Context
import android.webkit.WebView
import android.webkit.WebViewClient
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * Open a web view above the current activity.
 *
 * @param url     the url to open
 */
fun Context.displayInWebView(url: String) {
    val wv = WebView(this)

    // Set a WebViewClient to ensure redirection is handled directly in the WebView
    wv.webViewClient = WebViewClient()

    wv.loadUrl(url)
    MaterialAlertDialogBuilder(this)
            .setView(wv)
            .setPositiveButton(android.R.string.ok, null)
            .show()
}
