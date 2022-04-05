/*
 * Copyright (c) 2022 New Vector Ltd
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

package im.vector.app.features.home.room.threads

import android.app.Activity
import android.text.Spanned
import androidx.core.text.HtmlCompat
import im.vector.app.R
import im.vector.app.core.resources.StringProvider
import im.vector.app.features.MainActivity
import im.vector.app.features.MainActivityArgs
import im.vector.app.features.settings.VectorPreferences
import org.matrix.android.sdk.internal.database.lightweight.LightweightSettingsStorage
import javax.inject.Inject

/**
 * The class is responsible for handling thread specific tasks
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
     * Generates and return an Html spanned string to be rendered especially in dialogs
     */
    fun getBetaEnableThreadsMessage(): Spanned {
        val learnMore = stringProvider.getString(R.string.action_learn_more)
        val learnMoreUrl = stringProvider.getString(R.string.threads_learn_more_url)
        val href = "<a href='$learnMoreUrl'>$learnMore</a>.<br><br>"
        val message = stringProvider.getString(R.string.threads_beta_enable_notice_message, href)
        return HtmlCompat.fromHtml(message, HtmlCompat.FROM_HTML_MODE_LEGACY)
    }
}
