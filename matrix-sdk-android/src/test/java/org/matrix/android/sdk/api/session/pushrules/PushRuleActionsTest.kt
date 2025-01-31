/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session.pushrules

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.matrix.android.sdk.MatrixTest
import org.matrix.android.sdk.api.session.pushrules.rest.PushRule
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
