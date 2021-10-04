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
     * Send a boolean response
     *
     * @param response  the response
     * @param eventData the modular data
     */
    fun sendBoolResponse(response: Boolean, eventData: JsonDict)

    /**
     * Send an integer response
     *
     * @param response  the response
     * @param eventData the modular data
     */
    fun sendIntegerResponse(response: Int, eventData: JsonDict)

    /**
     * Send an object response
     *
     * @param klass the class of the response
     * @param response  the response
     * @param eventData the modular data
     */
    fun <T> sendObjectResponse(type: Type, response: T?, eventData: JsonDict)

    /**
     * Send success
     *
     * @param eventData the modular data
     */
    fun sendSuccess(eventData: JsonDict)

    /**
     * Send an error
     *
     * @param message   the error message
     * @param eventData the modular data
     */
    fun sendError(message: String, eventData: JsonDict)

    interface Handler {
        /**
         * Triggered when a widget is posting
         */
        fun handleWidgetRequest(mediator: WidgetPostAPIMediator, eventData: JsonDict): Boolean
    }
}
