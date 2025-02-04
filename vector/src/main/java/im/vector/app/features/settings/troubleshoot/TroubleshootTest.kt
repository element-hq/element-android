/*
 * Copyright 2018-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package im.vector.app.features.settings.troubleshoot

import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.StringRes
import kotlin.properties.Delegates

abstract class TroubleshootTest(@StringRes val titleResId: Int) {

    data class TestParameters(
            val activityResultLauncher: ActivityResultLauncher<Intent>,
            val permissionResultLauncher: ActivityResultLauncher<Array<String>>
    )

    enum class TestStatus {
        NOT_STARTED,
        RUNNING,
        WAITING_FOR_USER,
        FAILED,
        SUCCESS
    }

    var description: String? = null

    var status: TestStatus by Delegates.observable(TestStatus.NOT_STARTED) { _, _, _ ->
        statusListener?.invoke(this)
    }

    var statusListener: ((TroubleshootTest) -> Unit)? = null

    var manager: NotificationTroubleshootTestManager? = null

    abstract fun perform(testParameters: TestParameters)

    fun isFinished(): Boolean = (status == TestStatus.FAILED || status == TestStatus.SUCCESS)

    var quickFix: TroubleshootQuickFix? = null

    abstract class TroubleshootQuickFix(@StringRes val title: Int) {
        abstract fun doFix()
    }

    open fun cancel() {
    }

    open fun onPushReceived() {
    }

    open fun onNotificationClicked() {
    }
}
