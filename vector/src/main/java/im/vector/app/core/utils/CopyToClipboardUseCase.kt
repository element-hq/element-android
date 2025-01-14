/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.core.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.core.content.getSystemService
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class CopyToClipboardUseCase @Inject constructor(
        @ApplicationContext private val context: Context,
) {

    fun execute(text: CharSequence) {
        context.getSystemService<ClipboardManager>()
                ?.setPrimaryClip(ClipData.newPlainText("", text))
    }
}
