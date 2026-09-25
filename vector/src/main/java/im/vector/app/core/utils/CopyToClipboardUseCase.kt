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
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.content.getSystemService
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class CopyToClipboardUseCase @Inject constructor(
        @ApplicationContext private val context: Context,
) {

    private val handler = Handler(Looper.getMainLooper())
    private val clearClipboardRunnable = Runnable {
        val clipboard = context.getSystemService<ClipboardManager>() ?: return@Runnable
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            clipboard.clearPrimaryClip()
        } else {
            clipboard.setPrimaryClip(ClipData.newPlainText("", ""))
        }
    }

    fun execute(text: CharSequence) {
        val clipboard = context.getSystemService<ClipboardManager>() ?: return
        clipboard.setPrimaryClip(ClipData.newPlainText("", text))
        // Cancel any previous clear task and schedule a new one from the latest copy time
        handler.removeCallbacks(clearClipboardRunnable)
        handler.postDelayed(clearClipboardRunnable, CLIPBOARD_CLEAR_DELAY_MS)
    }

    companion object {
        private const val CLIPBOARD_CLEAR_DELAY_MS = 30_000L
    }
}
