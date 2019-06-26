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

        Assert.assertNotNull("Should have parsed the rule", pushRule)
        Assert.assertNotNull("Failed to parse actions", Action.mapFrom(pushRule!!))

        val actions = Action.mapFrom(pushRule)
        Assert.assertEquals(3, actions!!.size)


        Assert.assertEquals("First action should be notify", Action.Type.NOTIFY, actions[0].type)


        Assert.assertEquals("Second action should be tweak", Action.Type.SET_TWEAK, actions[1].type)
        Assert.assertEquals("Second action tweak key should be sound", "sound", actions[1].tweak_action)
        Assert.assertEquals("Second action should have default as stringValue", "default", actions[1].stringValue)
        Assert.assertNull("Second action boolValue should be null", actions[1].boolValue)


        Assert.assertEquals("Third action should be tweak", Action.Type.SET_TWEAK, actions[2].type)
        Assert.assertEquals("Third action tweak key should be highlight", "highlight", actions[2].tweak_action)
        Assert.assertEquals("Third action tweak param should be false", false, actions[2].boolValue)
        Assert.assertNull("Third action stringValue should be null", actions[2].stringValue)

    }
}