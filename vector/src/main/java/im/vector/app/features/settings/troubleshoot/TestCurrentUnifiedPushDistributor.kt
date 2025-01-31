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
import im.vector.app.core.pushers.UnifiedPushHelper
import im.vector.app.core.resources.StringProvider
import javax.inject.Inject

class TestCurrentUnifiedPushDistributor @Inject constructor(
        private val unifiedPushHelper: UnifiedPushHelper,
        private val stringProvider: StringProvider,
) : TroubleshootTest(R.string.settings_troubleshoot_test_current_distributor_title) {

    override fun perform(activityResultLauncher: ActivityResultLauncher<Intent>) {
        description = stringProvider.getString(
                R.string.settings_troubleshoot_test_current_distributor,
                unifiedPushHelper.getCurrentDistributorName()
        )
        status = TestStatus.SUCCESS
    }
}
