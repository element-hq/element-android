/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.lib.multipicker.entity

import android.net.Uri

data class MultiPickerAudioType(
        override val displayName: String?,
        override val size: Long,
        override val mimeType: String?,
        override val contentUri: Uri,
        val duration: Long,
        var waveform: List<Int>? = null
) : MultiPickerBaseType
