/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.api.pushrules

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.matrix.android.sdk.MatrixTest
import org.matrix.android.sdk.api.pushrules.rest.PushRule
import org.matrix.android.sdk.internal.di.MoshiProvider

class PushRuleActionsTest : MatrixTest {

    @Test
    fun test_action_parsing() {
        val rawPushRule = """
            {
                "rule_id": ".m.rule.invite_for_me",
                "default": true,
                "enabled": true,
                "conditions": [
                    {
                        "key": "type",
                        "kind": "event_match",
                        "pattern": "m.room.member"
                    },
                    {
                        "key": "content.membership",
                        "kind": "event_match",
                        "pattern": "invite"
                    },
                    {
                        "key": "state_key",
                        "kind": "event_match",
                        "pattern": "[the user's Matrix ID]"
                    }
                ],
                "actions": [
                   "notify",
                    {
                        "set_tweak": "sound",
                        "value": "default"
                    },
                    {
                        "set_tweak": "highlight",
                        "value": false
                    }
                ]
            }
        """.trimIndent()

        val pushRule = MoshiProvider.providesMoshi().adapter<PushRule>(PushRule::class.java).fromJson(rawPushRule)

        assertNotNull("Should have parsed the rule", pushRule)

        val actions = pushRule!!.getActions()
        assertEquals(3, actions.size)

        assertTrue("First action should be notify", actions[0] is Action.Notify)

        assertTrue("Second action should be sound", actions[1] is Action.Sound)
        assertEquals("Second action should have default sound", "default", (actions[1] as Action.Sound).sound)

        assertTrue("Third action should be highlight", actions[2] is Action.Highlight)
        assertEquals("Third action tweak param should be false", false, (actions[2] as Action.Highlight).highlight)
    }
}
