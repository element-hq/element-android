/*
 * Copyright 2018 New Vector Ltd
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
     * @param url         The url that failed.
     * @param errorCode   The error code.
     * @param description The error description.
     */
    fun onPageError(url: String, errorCode: Int, description: String) {
        // NO-OP
    }

    /**
     * Triggered when an error occurred while loading a page.
     *
     * @param url         The url that failed.
     * @param errorCode   The error code.
     * @param description The error description.
     */
    fun onHttpError(url: String, errorCode: Int, description: String) {
        // NO-OP
    }

    /**
     * Triggered when a webview load an url
     *
     * @param url The url about to be rendered.
     * @return true if the method needs to manage some custom handling
     */
    fun shouldOverrideUrlLoading(url: String): Boolean {
        return false
    }
}
