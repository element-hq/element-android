/*
 * Copyright 2022-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.settings.troubleshoot

import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import im.vector.app.R
import im.vector.app.core.pushers.FcmHelper
import im.vector.app.core.pushers.UnifiedPushHelper
import im.vector.app.core.resources.StringProvider
import javax.inject.Inject

class TestAvailableUnifiedPushDistributors @Inject constructor(
        private val unifiedPushHelper: UnifiedPushHelper,
        private val stringProvider: StringProvider,
        private val fcmHelper: FcmHelper,
) : TroubleshootTest(R.string.settings_troubleshoot_test_distributors_title) {

    override fun perform(activityResultLauncher: ActivityResultLauncher<Intent>) {
        val distributors = unifiedPushHelper.getExternalDistributors()
        description = if (distributors.isEmpty()) {
            stringProvider.getString(
                    if (fcmHelper.isFirebaseAvailable()) {
                        R.string.settings_troubleshoot_test_distributors_gplay
                    } else {
                        R.string.settings_troubleshoot_test_distributors_fdroid
                    }
            )
        } else {
            val quantity = distributors.size + 1
            stringProvider.getQuantityString(R.plurals.settings_troubleshoot_test_distributors_many, quantity, quantity)
        }
        status = TestStatus.SUCCESS
    }
}
