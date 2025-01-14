/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.lib.multipicker.entity

import android.net.Uri

interface MultiPickerBaseType {
    val displayName: String?
    val size: Long
    val mimeType: String?
    val contentUri: Uri
}
