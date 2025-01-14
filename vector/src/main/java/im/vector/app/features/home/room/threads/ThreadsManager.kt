/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.home.room.threads

import android.app.Activity
import android.text.Spanned
import androidx.annotation.StringRes
import androidx.core.text.HtmlCompat
import im.vector.app.core.resources.StringProvider
import im.vector.app.features.MainActivity
import im.vector.app.features.MainActivityArgs
import im.vector.app.features.settings.VectorPreferences
import im.vector.lib.strings.CommonStrings
import org.matrix.android.sdk.api.settings.LightweightSettingsStorage
import javax.inject.Inject

/**
 * The class is responsible for handling thread specific tasks.
 */
class ThreadsManager @Inject constructor(
        private val vectorPreferences: VectorPreferences,
        private val lightweightSettingsStorage: LightweightSettingsStorage,
        private val stringProvider: StringProvider
) {

    /**
     * Enable threads and invoke an initial sync. The initial sync is mandatory in order to change
     * the already saved DB schema for already received messages
     */
    fun enableThreadsAndRestart(activity: Activity) {
        vectorPreferences.setThreadMessagesEnabled()
        lightweightSettingsStorage.setThreadMessagesEnabled(vectorPreferences.areThreadMessagesEnabled())
        MainActivity.restartApp(activity, MainActivityArgs(clearCache = true))
    }

    /**
     * Generates and return an Html spanned string to be rendered especially in dialogs.
     */
    private fun generateLearnMoreHtmlString(@StringRes messageId: Int): Spanned {
        val learnMore = stringProvider.getString(CommonStrings.action_learn_more)
        val learnMoreUrl = stringProvider.getString(im.vector.app.config.R.string.threads_learn_more_url)
        val href = "<a href='$learnMoreUrl'>$learnMore</a>.<br><br>"
        val message = stringProvider.getString(messageId, href)
        return HtmlCompat.fromHtml(message, HtmlCompat.FROM_HTML_MODE_LEGACY)
    }

    fun getBetaEnableThreadsMessage(): Spanned =
            generateLearnMoreHtmlString(CommonStrings.threads_beta_enable_notice_message)

    fun getLabsEnableThreadsMessage(): Spanned =
            generateLearnMoreHtmlString(CommonStrings.threads_labs_enable_notice_message)
}
