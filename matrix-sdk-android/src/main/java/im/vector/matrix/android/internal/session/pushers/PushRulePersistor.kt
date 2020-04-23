package im.vector.matrix.android.internal.session.pushers

import im.vector.matrix.android.api.pushrules.RuleScope
import im.vector.matrix.android.api.pushrules.RuleSetKey
import im.vector.matrix.android.api.pushrules.rest.GetPushRulesResponse
import im.vector.matrix.android.api.pushrules.rest.PushRule
import im.vector.matrix.android.internal.database.mapper.PushConditionMapper
import im.vector.matrix.android.internal.database.mapper.PushRulesMapper
import im.vector.matrix.sqldelight.session.PushRuleQueries
import im.vector.matrix.sqldelight.session.SessionDatabase
import javax.inject.Inject

internal class PushRulePersistor @Inject constructor(private val sessionDatabase: SessionDatabase,
                                                     private val pushRulesMapper: PushRulesMapper,
                                                     private val pushConditionMapper: PushConditionMapper) {

    fun persist(pushRules: GetPushRulesResponse) = sessionDatabase.transaction {
        // clear current push rules
        sessionDatabase.pushRuleQueries.deleteAllPushRules()
        // Save only global rules for the moment
        val globalRules = pushRules.global

        sessionDatabase.pushRuleQueries.insertPushRules(RuleScope.GLOBAL, RuleSetKey.CONTENT, globalRules.content)
        sessionDatabase.pushRuleQueries.insertPushRules(RuleScope.GLOBAL, RuleSetKey.OVERRIDE, globalRules.override)
        sessionDatabase.pushRuleQueries.insertPushRules(RuleScope.GLOBAL, RuleSetKey.ROOM, globalRules.room)
        sessionDatabase.pushRuleQueries.insertPushRules(RuleScope.GLOBAL, RuleSetKey.SENDER, globalRules.sender)
        sessionDatabase.pushRuleQueries.insertPushRules(RuleScope.GLOBAL, RuleSetKey.UNDERRIDE, globalRules.underride)
    }

    private fun PushRuleQueries.insertPushRules(scope: String, kind: RuleSetKey, pushRules: List<PushRule>?) {
        pushRules?.forEach { rule ->
            val pushRuleEntity = pushRulesMapper.map(scope, kind, rule)
            insertPushRule(pushRuleEntity)
            rule.conditions?.forEach { condition ->
                val pushConditionEntity = pushConditionMapper.map(rule.ruleId, condition)
                insertPushCondition(pushConditionEntity)
            }

        }
    }

}
