/*
 * Copyright (c) 2020 New Vector Ltd
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

package im.vector.riotx.features.widgets

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import com.airbnb.mvrx.args
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import im.vector.fragments.roomwidgets.WebviewPermissionUtils
import im.vector.riotx.R
import im.vector.riotx.core.platform.VectorBaseFragment
import im.vector.riotx.features.themes.ThemeUtils
import im.vector.riotx.features.webview.VectorWebViewClient
import im.vector.riotx.features.webview.WebViewEventListener
import im.vector.riotx.features.widgets.webview.clearAfterWidget
import im.vector.riotx.features.widgets.webview.setupForWidget
import kotlinx.android.parcel.Parcelize
import kotlinx.android.synthetic.main.fragment_widget.*
import timber.log.Timber
import javax.inject.Inject

@Parcelize
data class WidgetArgs(
        val widgetId: String
) : Parcelable

class WidgetFragment @Inject constructor(
        private val viewModelFactory: RoomWidgetViewModel.Factory
) : VectorBaseFragment(), RoomWidgetViewModel.Factory by viewModelFactory, WebViewEventListener {

    private val fragmentArgs: WidgetArgs by args()
    private val viewModel: RoomWidgetViewModel by fragmentViewModel()

    override fun getLayoutResId() = R.layout.fragment_widget

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        widgetWebView.setupForWidget(this)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        widgetWebView.clearAfterWidget()
    }

    override fun onResume() {
        super.onResume()
        widgetWebView?.let {
            it.resumeTimers()
            it.onResume()
        }
    }

    override fun onPause() {
        super.onPause()
        widgetWebView?.let {
            it.pauseTimers()
            it.onPause()
        }
    }

    override fun invalidate() = withState(viewModel) { state ->
        Timber.v("Invalidate with state: $state")
    }

    override fun onPageStarted(url: String) {

    }

    override fun onPageFinished(url: String) {

    }

    override fun onPageError(url: String, errorCode: Int, description: String) {

    }

}
