/*
 * Copyright 2018-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.webview

interface WebViewEventListener {

    /**
     * Triggered when a webview page is about to be started.
     *
     * @param url The url about to be rendered.
     */
    fun pageWillStart(url: String) {
        // NO-OP
    }

    /**
     * Triggered when a loading webview page has started.
     *
     * @param url The rendering url.
     */
    fun onPageStarted(url: String) {
        // NO-OP
    }

    /**
     * Triggered when a loading webview page has finished loading but has not been rendered yet.
     *
     * @param url The finished url.
     */
    fun onPageFinished(url: String) {
        // NO-OP
    }

    /**
     * Triggered when an error occurred while loading a page.
     *
     * @param url The url that failed.
     * @param errorCode The error code.
     * @param description The error description.
     */
    fun onPageError(url: String, errorCode: Int, description: String) {
        // NO-OP
    }

    /**
     * Triggered when an error occurred while loading a page.
     *
     * @param url The url that failed.
     * @param errorCode The error code.
     * @param description The error description.
     */
    fun onHttpError(url: String, errorCode: Int, description: String) {
        // NO-OP
    }

    /**
     * Triggered when a webview load an url.
     *
     * @param url The url about to be rendered.
     * @return true if the method needs to manage some custom handling
     */
    fun shouldOverrideUrlLoading(url: String): Boolean {
        return false
    }
}
