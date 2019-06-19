package im.vector.matrix.android.internal.session.notification

import im.vector.matrix.android.api.pushrules.PushRulesProvider
import im.vector.matrix.android.api.pushrules.rest.PushRule
import im.vector.matrix.android.internal.di.MoshiProvider


class MockPushRuleProvider : PushRulesProvider {
    override fun getOrderedPushrules(): List<PushRule> {
        val raw = """
            {
                "actions": [
                  "notify",
                  {
                    "set_tweak": "highlight",
                    "value": false
                  }
                ],
                "conditions": [
                  {
                    "key": "type",
                    "kind": "event_match",
                    "pattern": "m.room.message"
                  }
                ],
                "default": true,
                "enabled": true,
                "rule_id": ".m.rule.message"
              }
        """.trimIndent()
        val pushRule = MoshiProvider.providesMoshi().adapter<PushRule>(PushRule::class.java).fromJson(raw)

        return listOf<PushRule>(
                pushRule!!
        )
    }

    override fun onRulesUpdate(newRules: List<PushRule>) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}