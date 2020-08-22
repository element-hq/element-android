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
import org.matrix.android.sdk.api.session.Session
import im.vector.app.R
import im.vector.app.core.di.ScreenComponent
import im.vector.app.core.platform.VectorBaseActivity
import kotlinx.android.synthetic.main.activity_vector_web_view.*
import javax.inject.Inject

/**
 * This class is responsible for managing a WebView
 * It does also have a loading view and a toolbar
 * It relies on the VectorWebViewClient
 * This class shouldn't be extended. To add new behaviors, you might create a new WebViewMode and a new WebViewEventListener
 */
class VectorWebViewActivity : VectorBaseActivity() {

    override fun getLayoutRes() = R.layout.activity_vector_web_view

    @Inject lateinit var session: Session

    @CallSuper
    override fun injectWith(injector: ScreenComponent) {
        session = injector.activeSessionHolder().getActiveSession()
    }

    override fun initUiAndData() {
        configureToolbar(webview_toolbar)
        waitingView = findViewById(R.id.simple_webview_loader)

        simple_webview.settings.apply {
            // Enable Javascript
            javaScriptEnabled = true

            // Use WideViewport and Zoom out if there is no viewport defined
            useWideViewPort = true
            loadWithOverviewMode = true

            // Enable pinch to zoom without the zoom buttons
            builtInZoomControls = true

            // Allow use of Local Storage
            domStorageEnabled = true

            allowFileAccessFromFileURLs = true
            allowUniversalAccessFromFileURLs = true

            displayZoomControls = false
        }

        val cookieManager = android.webkit.CookieManager.getInstance()
        cookieManager.setAcceptThirdPartyCookies(simple_webview, true)

        val url = intent.extras?.getString(EXTRA_URL)
        val title = intent.extras?.getString(EXTRA_TITLE, USE_TITLE_FROM_WEB_PAGE)
        if (title != USE_TITLE_FROM_WEB_PAGE) {
            setTitle(title)
        }

        val webViewMode = intent.extras?.getSerializable(EXTRA_MODE) as WebViewMode
        val eventListener = webViewMode.eventListener(this, session)
        simple_webview.webViewClient = VectorWebViewClient(eventListener)
        simple_webview.webChromeClient = object : WebChromeClient() {
            override fun onReceivedTitle(view: WebView, title: String) {
                if (title == USE_TITLE_FROM_WEB_PAGE) {
                    setTitle(title)
                }
            }
        }
        simple_webview.loadUrl(url)
    }

    /* ==========================================================================================
     * UI event
     * ========================================================================================== */

    override fun onBackPressed() {
        if (simple_webview.canGoBack()) {
            simple_webview.goBack()
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
