/*
 * Copyright 2018-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
