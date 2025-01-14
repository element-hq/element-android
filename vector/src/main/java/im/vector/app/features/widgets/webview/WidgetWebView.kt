/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.widgets.webview

import android.app.Activity
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebView
import im.vector.app.core.utils.CheckWebViewPermissionsUseCase
import im.vector.app.features.themes.ThemeUtils
import im.vector.app.features.webview.VectorWebViewClient
import im.vector.app.features.webview.WebEventListener

fun WebView.setupForWidget(activity: Activity,
                           checkWebViewPermissionsUseCase: CheckWebViewPermissionsUseCase,
                           eventListener: WebEventListener,
) {
    // xml value seems ignored
    setBackgroundColor(ThemeUtils.getColor(context, com.google.android.material.R.attr.colorSurface))

    // clear caches
    clearHistory()
    clearFormData()

    // Enable Javascript
    settings.javaScriptEnabled = true

    // Use WideViewport and Zoom out if there is no viewport defined
    settings.useWideViewPort = true
    settings.loadWithOverviewMode = true

    // Enable pinch to zoom without the zoom buttons
    settings.builtInZoomControls = true

    // Allow use of Local Storage
    settings.domStorageEnabled = true

    @Suppress("DEPRECATION")
    settings.allowFileAccessFromFileURLs = true
    @Suppress("DEPRECATION")
    settings.allowUniversalAccessFromFileURLs = true

    settings.displayZoomControls = false

    settings.mediaPlaybackRequiresUserGesture = false

    // Permission requests
    webChromeClient = object : WebChromeClient() {
        override fun onPermissionRequest(request: PermissionRequest) {
            if (checkWebViewPermissionsUseCase.execute(activity, request)) {
                request.grant(request.resources)
            } else {
                eventListener.onPermissionRequest(request)
            }
        }
    }
    webViewClient = VectorWebViewClient(eventListener)

    val cookieManager = CookieManager.getInstance()
    cookieManager.setAcceptThirdPartyCookies(this, false)
}

fun WebView.clearAfterWidget() {
    // Make sure you remove the WebView from its parent view before doing anything.
    (parent as? ViewGroup)?.removeAllViews()
    webChromeClient = null
    clearHistory()
    // Loading a blank page is optional, but will ensure that the WebView isn't doing anything when you destroy it.
    loadUrl("about:blank")
    removeAllViews()
    destroy()
}
