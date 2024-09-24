/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 * Please see LICENSE in the repository root for full details.
 */

package im.vector.app.features.webview

import android.webkit.PermissionRequest

interface WebChromeEventListener {

    /**
     * Triggered when the web view requests permissions.
     *
     * @param request The permission request.
     */
    fun onPermissionRequest(request: PermissionRequest)
}
