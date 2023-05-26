/*
 * Copyright (c) 2023 New Vector Ltd
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

package im.vector.app.features.settings.notifications.usecase

import im.vector.app.test.fakes.FakeSession
import im.vector.app.test.fixtures.PushRuleFixture
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.amshove.kluent.shouldBeEqualTo
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.matrix.android.sdk.api.session.pushrules.Action
import org.matrix.android.sdk.api.session.pushrules.RuleIds
import org.matrix.android.sdk.api.session.pushrules.rest.PushRule

internal class GetPushRulesOnInvalidStateUseCaseTest {

    private val fakeSession = FakeSession()
    private val getPushRulesOnInvalidStateUseCase = GetPushRulesOnInvalidStateUseCase()

    @Before
    fun setup() {
        mockkStatic("org.matrix.android.sdk.api.session.pushrules.ActionKt")
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `given a list of push rules with children not matching their parent when execute then returns the list of not matching rules`() {
        // Given
        val firstActions = listOf(Action.Notify)
        val secondActions = listOf(Action.DoNotNotify)
        givenARuleList(
                listOf(
                        // first set of related rules
                        givenARuleId(RuleIds.RULE_ID_ONE_TO_ONE_ROOM, true, firstActions),
                        givenARuleId(RuleIds.RULE_ID_POLL_START_ONE_TO_ONE, true, listOf(Action.DoNotNotify)), // diff
                        givenARuleId(RuleIds.RULE_ID_POLL_START_ONE_TO_ONE_UNSTABLE, true, emptyList()), // diff
                        givenARuleId(RuleIds.RULE_ID_POLL_END_ONE_TO_ONE, false, listOf(Action.Notify)), // diff
                        givenARuleId(RuleIds.RULE_ID_POLL_END_ONE_TO_ONE_UNSTABLE, true, listOf(Action.Notify)),
                        // second set of related rules
                        givenARuleId(RuleIds.RULE_ID_ALL_OTHER_MESSAGES_ROOMS, false, secondActions),
                        givenARuleId(RuleIds.RULE_ID_POLL_START, true, listOf(Action.Notify)), // diff
                        givenARuleId(RuleIds.RULE_ID_POLL_START_UNSTABLE, false, listOf(Action.DoNotNotify)),
                        givenARuleId(RuleIds.RULE_ID_POLL_END, false, listOf(Action.Notify)), // diff
                        givenARuleId(RuleIds.RULE_ID_POLL_END_UNSTABLE, true, listOf()), // diff
                        // Another rule
                        givenARuleId(RuleIds.RULE_ID_CONTAIN_USER_NAME, true, listOf(Action.Notify)),
                )
        )

        // When
        val result = getPushRulesOnInvalidStateUseCase.execute(fakeSession).map { it.ruleId }

        // Then
        result shouldBeEqualTo listOf(
                RuleIds.RULE_ID_ONE_TO_ONE_ROOM, // parent rule
                RuleIds.RULE_ID_POLL_START_ONE_TO_ONE,
                RuleIds.RULE_ID_POLL_START_ONE_TO_ONE_UNSTABLE,
                RuleIds.RULE_ID_POLL_END_ONE_TO_ONE,
                RuleIds.RULE_ID_ALL_OTHER_MESSAGES_ROOMS, // parent rule
                RuleIds.RULE_ID_POLL_START,
                RuleIds.RULE_ID_POLL_END,
                RuleIds.RULE_ID_POLL_END_UNSTABLE,
        )
    }

    private fun givenARuleList(rules: List<PushRule>) {
        every { fakeSession.fakePushRuleService.getPushRules().getAllRules() } returns rules
    }

    private fun givenARuleId(ruleId: String, enabled: Boolean, actions: List<Action>): PushRule {
        val ruleAndKind = PushRuleFixture.aPushRuleAndKind(
                PushRuleFixture.aPushRule(ruleId, enabled, actions),
        )

        every { fakeSession.fakePushRuleService.getPushRules().findDefaultRule(ruleId) } returns ruleAndKind

        return ruleAndKind.pushRule
    }
}
