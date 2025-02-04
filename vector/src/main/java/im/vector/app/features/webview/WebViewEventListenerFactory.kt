/*
 * Copyright 2018-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.webview

import im.vector.app.core.platform.VectorBaseActivity
import org.matrix.android.sdk.api.session.Session

interface WebViewEventListenerFactory {

    /**
     * @return an instance of WebViewEventListener
     */
    fun eventListener(activity: VectorBaseActivity<*>, session: Session): WebViewEventListener
}
