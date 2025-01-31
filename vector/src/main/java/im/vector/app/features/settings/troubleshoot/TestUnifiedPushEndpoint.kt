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

class TestUnifiedPushEndpoint @Inject constructor(
        private val stringProvider: StringProvider,
        private val unifiedPushHelper: UnifiedPushHelper,
) : TroubleshootTest(R.string.settings_troubleshoot_test_current_endpoint_title) {

    override fun perform(activityResultLauncher: ActivityResultLauncher<Intent>) {
        val endpoint = unifiedPushHelper.getPrivacyFriendlyUpEndpoint()
        if (endpoint != null) {
            description = stringProvider.getString(R.string.settings_troubleshoot_test_current_endpoint_success, endpoint)
            status = TestStatus.SUCCESS
        } else {
            description = stringProvider.getString(R.string.settings_troubleshoot_test_current_endpoint_failed)
            status = TestStatus.FAILED
        }
    }
}
