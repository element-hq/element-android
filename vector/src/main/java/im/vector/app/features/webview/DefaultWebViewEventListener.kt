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

import timber.log.Timber

/**
 * This class is the default implementation of WebViewEventListener.
 * It can be used with delegation pattern
 */

class DefaultWebViewEventListener : WebViewEventListener {

    override fun pageWillStart(url: String) {
        Timber.v("On page will start: $url")
    }

    override fun onPageStarted(url: String) {
        Timber.d("On page started: $url")
    }

    override fun onPageFinished(url: String) {
        Timber.d("On page finished: $url")
    }

    override fun onPageError(url: String, errorCode: Int, description: String) {
        Timber.e("On received error: $url - errorCode: $errorCode - message: $description")
    }

    override fun shouldOverrideUrlLoading(url: String): Boolean {
        Timber.v("Should override url: $url")
        return false
    }
}
