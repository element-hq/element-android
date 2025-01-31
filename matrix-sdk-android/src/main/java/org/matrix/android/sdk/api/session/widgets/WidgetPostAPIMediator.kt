/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session.widgets

import android.webkit.WebView
import org.matrix.android.sdk.api.util.JsonDict
import java.lang.reflect.Type

interface WidgetPostAPIMediator {

    /**
     * This initialize the webview to handle.
     * It will add a JavaScript Interface.
     * Please call [clearWebView] method when finished to clean the provided webview
     */
    fun setWebView(webView: WebView)

    /**
     * Set handler to communicate with the widgetPostAPIMediator.
     * Please remove the reference by passing null when finished.
     */
    fun setHandler(handler: Handler?)

    /**
     * This clear the mediator by removing the JavaScript Interface and cleaning references.
     */
    fun clearWebView()

    /**
     * Inject the necessary javascript into the configured WebView.
     * Should be called after a web page has been loaded.
     */
    fun injectAPI()

    /**
     * Send a boolean response.
     *
     * @param response the response
     * @param eventData the modular data
     */
    fun sendBoolResponse(response: Boolean, eventData: JsonDict)

    /**
     * Send an integer response.
     *
     * @param response the response
     * @param eventData the modular data
     */
    fun sendIntegerResponse(response: Int, eventData: JsonDict)

    /**
     * Send an object response.
     *
     * @param T the generic type
     * @param type the type of the response
     * @param response the response
     * @param eventData the modular data
     */
    fun <T> sendObjectResponse(type: Type, response: T?, eventData: JsonDict)

    /**
     * Send success.
     *
     * @param eventData the modular data
     */
    fun sendSuccess(eventData: JsonDict)

    /**
     * Send an error.
     *
     * @param message the error message
     * @param eventData the modular data
     */
    fun sendError(message: String, eventData: JsonDict)

    interface Handler {
        /**
         * Triggered when a widget is posting.
         */
        fun handleWidgetRequest(mediator: WidgetPostAPIMediator, eventData: JsonDict): Boolean
    }
}
