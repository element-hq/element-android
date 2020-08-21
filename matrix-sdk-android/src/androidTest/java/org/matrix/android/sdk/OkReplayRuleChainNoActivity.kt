/*
 * Copyright 2019 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.matrix.android.sdk

import okreplay.OkReplayConfig
import okreplay.PermissionRule
import okreplay.RecorderRule
import org.junit.rules.RuleChain
import org.junit.rules.TestRule

class OkReplayRuleChainNoActivity(
        private val configuration: OkReplayConfig) {

    fun get(): TestRule {
        return RuleChain.outerRule(PermissionRule(configuration))
                .around(RecorderRule(configuration))
    }
}
