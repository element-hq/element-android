/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.settings.troubleshoot

import im.vector.app.core.pushers.UnifiedPushHelper
import im.vector.app.core.resources.StringProvider
import im.vector.lib.strings.CommonStrings
import javax.inject.Inject

class TestUnifiedPushGateway @Inject constructor(
        private val unifiedPushHelper: UnifiedPushHelper,
        private val stringProvider: StringProvider
) : TroubleshootTest(CommonStrings.settings_troubleshoot_test_current_gateway_title) {

    override fun perform(testParameters: TestParameters) {
        description = stringProvider.getString(
                CommonStrings.settings_troubleshoot_test_current_gateway,
                unifiedPushHelper.getPushGateway()
        )
        status = TestStatus.SUCCESS
    }
}
