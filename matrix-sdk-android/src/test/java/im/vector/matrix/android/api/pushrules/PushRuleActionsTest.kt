package im.vector.matrix.android.api.pushrules

import im.vector.matrix.android.api.pushrules.rest.PushRule
import im.vector.matrix.android.internal.di.MoshiProvider
import org.junit.Assert
import org.junit.Test


class PushRuleActionsTest {

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
                "domainActions": [
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

        Assert.assertNotNull("Should have parsed the rule", pushRule)
        Assert.assertNotNull("Failed to parse domainActions", pushRule?.domainActions())
        Assert.assertEquals(3, pushRule!!.domainActions()!!.size)


        Assert.assertEquals("First action should be notify", Action.Type.NOTIFY, pushRule!!.domainActions()!![0].type)


        Assert.assertEquals("Second action should be tweak", Action.Type.SET_TWEAK, pushRule!!.domainActions()!![1].type)
        Assert.assertEquals("Second action tweak key should be sound", "sound", pushRule!!.domainActions()!![1].tweak_action)
        Assert.assertEquals("Second action should have default as stringValue", "default", pushRule!!.domainActions()!![1].stringValue)
        Assert.assertNull("Second action boolValue should be null",  pushRule!!.domainActions()!![1].boolValue)


        Assert.assertEquals("Third action should be tweak", Action.Type.SET_TWEAK, pushRule!!.domainActions()!![2].type)
        Assert.assertEquals("Third action tweak key should be highlight", "highlight", pushRule!!.domainActions()!![2].tweak_action)
        Assert.assertEquals("Third action tweak param should be false", false, pushRule!!.domainActions()!![2].boolValue)
        Assert.assertNull("Third action stringValue should be null",  pushRule!!.domainActions()!![2].stringValue)

    }
}