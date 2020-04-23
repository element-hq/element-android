package im.vector.matrix.android.internal.session.pushers

import com.squareup.sqldelight.runtime.coroutines.asFlow
import im.vector.matrix.android.api.pushrules.RuleKind
import im.vector.matrix.android.api.pushrules.RuleScope
import im.vector.matrix.android.api.pushrules.RuleSetKey
import im.vector.matrix.android.api.pushrules.rest.RuleSet
import im.vector.matrix.android.api.util.Optional
import im.vector.matrix.android.api.util.toOptional
import im.vector.matrix.android.internal.database.mapper.PushRulesMapper
import im.vector.matrix.android.internal.session.room.notification.RoomPushRule
import im.vector.matrix.sqldelight.session.PushRuleEntity
import im.vector.matrix.sqldelight.session.SessionDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

internal class PushRuleDataSource @Inject constructor(private val sessionDatabase: SessionDatabase,
                                                      private val pushRulesMapper: PushRulesMapper) {

    fun getPushRules(scope: String): RuleSet {
        val contentRules = sessionDatabase.pushRuleQueries.getPushRuleWithKind(scope, RuleSetKey.CONTENT.name)
                .executeAsList()
                .map {
                    pushRulesMapper.mapContentRule(it)
                }

        val overrideRules = sessionDatabase.pushRuleQueries.getPushRuleWithKind(scope, RuleSetKey.OVERRIDE.name)
                .executeAsList()
                .map {
                    val conditions = sessionDatabase.pushRuleQueries.getPushConditions(it.rule_id).executeAsList()
                    pushRulesMapper.map(it, conditions)
                }

        val roomRules = sessionDatabase.pushRuleQueries.getPushRuleWithKind(scope, RuleSetKey.ROOM.name)
                .executeAsList()
                .map {
                    pushRulesMapper.mapRoomRule(it)
                }

        val senderRules = sessionDatabase.pushRuleQueries.getPushRuleWithKind(scope, RuleSetKey.SENDER.name)
                .executeAsList()
                .map {
                    pushRulesMapper.mapSenderRule(it)
                }

        val underrideRules = sessionDatabase.pushRuleQueries.getPushRuleWithKind(scope, RuleSetKey.UNDERRIDE.name)
                .executeAsList()
                .map {
                    val conditions = sessionDatabase.pushRuleQueries.getPushConditions(it.rule_id).executeAsList()
                    pushRulesMapper.map(it, conditions)
                }

        return RuleSet(
                content = contentRules,
                override = overrideRules,
                room = roomRules,
                sender = senderRules,
                underride = underrideRules
        )
    }

    fun getRoomPushRule(roomId: String): RoomPushRule? {
        return sessionDatabase.pushRuleQueries.getPushRuleWithId(RuleScope.GLOBAL, ruleId = roomId)
                .executeAsOneOrNull()
                .toRoomPushRule()
    }


    fun getRoomPushRuleLive(roomId: String): Flow<Optional<RoomPushRule>> {
        return sessionDatabase.pushRuleQueries.getPushRuleWithId(RuleScope.GLOBAL, ruleId = roomId)
                .asFlow()
                .map { query ->
                    query.executeAsOneOrNull().toRoomPushRule()

                }
                .map { it.toOptional() }
    }

    private fun PushRuleEntity?.toRoomPushRule(): RoomPushRule? {
        if (this == null) {
            return null
        }
        val kind = RuleKind.valueOf(kind)
        val pushRule = when (kind) {
            RuleSetKey.OVERRIDE -> {
                pushRulesMapper.map(this, emptyList())
            }
            RuleSetKey.ROOM -> {
                pushRulesMapper.mapRoomRule(this)
            }
            else -> null
        }
        return if (pushRule == null) {
            null
        } else {
            RoomPushRule(kind, pushRule)
        }
    }

}
