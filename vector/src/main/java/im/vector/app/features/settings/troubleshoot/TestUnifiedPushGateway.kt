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
