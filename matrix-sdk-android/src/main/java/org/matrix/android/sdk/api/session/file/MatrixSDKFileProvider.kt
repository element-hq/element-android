/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session.file

import android.net.Uri
import androidx.core.content.FileProvider

/**
 * We have to declare our own file provider to avoid collision with apps using the sdk
 * and having their own.
 */
class MatrixSDKFileProvider : FileProvider() {
    override fun getType(uri: Uri): String? {
        return super.getType(uri) ?: "plain/text"
    }
}
