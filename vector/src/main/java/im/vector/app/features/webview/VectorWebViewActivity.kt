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

import android.content.Context
import android.content.Intent
import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.annotation.CallSuper
import im.vector.app.core.di.ScreenComponent
import im.vector.app.core.platform.VectorBaseActivity
import im.vector.app.databinding.ActivityVectorWebViewBinding
import org.matrix.android.sdk.api.session.Session
import javax.inject.Inject

/**
 * This class is responsible for managing a WebView
 * It does also have a loading view and a toolbar
 * It relies on the VectorWebViewClient
 * This class shouldn't be extended. To add new behaviors, you might create a new WebViewMode and a new WebViewEventListener
 */
class VectorWebViewActivity : VectorBaseActivity<ActivityVectorWebViewBinding>() {

    override fun getBinding() = ActivityVectorWebViewBinding.inflate(layoutInflater)

    @Inject lateinit var session: Session

    @CallSuper
    override fun injectWith(injector: ScreenComponent) {
        session = injector.activeSessionHolder().getActiveSession()
    }

    override fun initUiAndData() {
        configureToolbar(views.webviewToolbar)
        waitingView = views.simpleWebviewLoader

        views.simpleWebview.settings.apply {
            // Enable Javascript
            javaScriptEnabled = true

            // Use WideViewport and Zoom out if there is no viewport defined
            useWideViewPort = true
            loadWithOverviewMode = true

            // Enable pinch to zoom without the zoom buttons
            builtInZoomControls = true

            // Allow use of Local Storage
            domStorageEnabled = true

            @Suppress("DEPRECATION")
            allowFileAccessFromFileURLs = true
            @Suppress("DEPRECATION")
            allowUniversalAccessFromFileURLs = true

            displayZoomControls = false
        }

        val cookieManager = android.webkit.CookieManager.getInstance()
        cookieManager.setAcceptThirdPartyCookies(views.simpleWebview, true)

        val url = intent.extras?.getString(EXTRA_URL) ?: return
        val title = intent.extras?.getString(EXTRA_TITLE, USE_TITLE_FROM_WEB_PAGE)
        if (title != USE_TITLE_FROM_WEB_PAGE) {
            setTitle(title)
        }

        val webViewMode = intent.extras?.getSerializable(EXTRA_MODE) as WebViewMode
        val eventListener = webViewMode.eventListener(this, session)
        views.simpleWebview.webViewClient = VectorWebViewClient(eventListener)
        views.simpleWebview.webChromeClient = object : WebChromeClient() {
            override fun onReceivedTitle(view: WebView, title: String) {
                if (title == USE_TITLE_FROM_WEB_PAGE) {
                    setTitle(title)
                }
            }
        }
        views.simpleWebview.loadUrl(url)
    }

    /* ==========================================================================================
     * UI event
     * ========================================================================================== */

    override fun onBackPressed() {
        if (views.simpleWebview.canGoBack()) {
            views.simpleWebview.goBack()
        } else {
            super.onBackPressed()
        }
    }

    /* ==========================================================================================
     * Companion
     * ========================================================================================== */

    companion object {
        private const val EXTRA_URL = "EXTRA_URL"
        private const val EXTRA_TITLE = "EXTRA_TITLE"
        private const val EXTRA_MODE = "EXTRA_MODE"

        private const val USE_TITLE_FROM_WEB_PAGE = ""

        fun getIntent(context: Context,
                      url: String,
                      title: String = USE_TITLE_FROM_WEB_PAGE,
                      mode: WebViewMode = WebViewMode.DEFAULT): Intent {
            return Intent(context, VectorWebViewActivity::class.java)
                    .apply {
                        putExtra(EXTRA_URL, url)
                        putExtra(EXTRA_TITLE, title)
                        putExtra(EXTRA_MODE, mode)
                    }
        }
    }
}
