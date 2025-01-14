/*
 * Copyright 2018-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.webview

import im.vector.app.core.platform.VectorBaseActivity
import org.matrix.android.sdk.api.session.Session

/**
 * This enum indicates the WebView mode. It's responsible for creating a WebViewEventListener
 */
enum class WebViewMode : WebViewEventListenerFactory {

    DEFAULT {
        override fun eventListener(activity: VectorBaseActivity<*>, session: Session): WebViewEventListener {
            return DefaultWebViewEventListener()
        }
    },
    CONSENT {
        override fun eventListener(activity: VectorBaseActivity<*>, session: Session): WebViewEventListener {
            return ConsentWebViewEventListener(activity, session, DefaultWebViewEventListener())
        }
    };
}
