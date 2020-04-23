/*
 * Copyright 2019 New Vector Ltd
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
package im.vector.matrix.android.internal.database.mapper

import com.squareup.moshi.Types
import im.vector.matrix.android.api.pushrules.Condition
import im.vector.matrix.android.api.pushrules.RuleKind
import im.vector.matrix.android.api.pushrules.rest.PushCondition
import im.vector.matrix.android.api.pushrules.rest.PushRule
import im.vector.matrix.android.internal.di.MoshiProvider
import im.vector.matrix.sqldelight.session.GetPushConditions
import im.vector.matrix.sqldelight.session.PushRuleEntity
import timber.log.Timber
import javax.inject.Inject

internal class PushRulesMapper @Inject constructor(private val pushConditionMapper: PushConditionMapper) {

    private val moshiActionsAdapter = MoshiProvider.providesMoshi().adapter<List<Any>>(Types.newParameterizedType(List::class.java, Any::class.java))

//    private val listOfAnyAdapter: JsonAdapter<List<Any>> =
//            moshi.adapter<List<Any>>(Types.newParameterizedType(List::class.java, Any::class.java), kotlin.collections.emptySet(), "actions")

    fun mapContentRule(pushrule: PushRuleEntity): PushRule {
        return PushRule(
                actions = fromActionStr(pushrule.action_str),
                default = pushrule.is_default,
                enabled = pushrule.is_enabled,
                ruleId = pushrule.rule_id,
                conditions = listOf(
                        PushCondition(Condition.Kind.EventMatch.value, "content.body", pushrule.pattern)
                )
        )
    }

    private fun fromActionStr(actionsStr: String?): List<Any> {
        return try {
            actionsStr?.let { moshiActionsAdapter.fromJson(it) } ?: emptyList()
        } catch (e: Throwable) {
            Timber.e(e, "## failed to map push rule actions <$actionsStr>")
            emptyList()
        }
    }

    fun mapRoomRule(pushrule: PushRuleEntity): PushRule {
        return PushRule(
                actions = fromActionStr(pushrule.action_str),
                default = pushrule.is_default,
                enabled = pushrule.is_enabled,
                ruleId = pushrule.rule_id,
                conditions = listOf(
                        PushCondition(Condition.Kind.EventMatch.value, "room_id", pushrule.rule_id)
                )
        )
    }

    fun mapSenderRule(pushrule: PushRuleEntity): PushRule {
        return PushRule(
                actions = fromActionStr(pushrule.action_str),
                default = pushrule.is_default,
                enabled = pushrule.is_enabled,
                ruleId = pushrule.rule_id,
                conditions = listOf(
                        PushCondition(Condition.Kind.EventMatch.value, "user_id", pushrule.rule_id)
                )
        )
    }

    fun map(pushrule: PushRuleEntity, conditions: List<GetPushConditions>): PushRule {
        return PushRule(
                actions = fromActionStr(pushrule.action_str),
                default = pushrule.is_default,
                enabled = pushrule.is_enabled,
                ruleId = pushrule.rule_id,
                conditions = conditions.map {
                    pushConditionMapper.map(it)
                }
        )
    }

    fun map(scope: String, kind: RuleKind, pushRule: PushRule): PushRuleEntity {
        return PushRuleEntity.Impl(
                action_str = moshiActionsAdapter.toJson(pushRule.actions),
                is_default = pushRule.default ?: false,
                is_enabled = pushRule.enabled,
                rule_id = pushRule.ruleId,
                pattern = pushRule.pattern,
                scope = scope,
                kind = kind.name
        )
    }

}
