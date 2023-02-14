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
import io.mockk.coVerifySequence
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.matrix.android.sdk.api.session.pushrules.Action
import org.matrix.android.sdk.api.session.pushrules.RuleIds
import org.matrix.android.sdk.api.session.pushrules.getActions
import org.matrix.android.sdk.api.session.pushrules.rest.PushRule
import org.matrix.android.sdk.api.session.pushrules.rest.PushRuleAndKind

internal class UpdatePushRulesIfNeededUseCaseTest {

    private val fakeSession = FakeSession()
    private val updatePushRulesIfNeededUseCase = UpdatePushRulesIfNeededUseCase()

    @Before
    fun setup() {
        mockkStatic("org.matrix.android.sdk.api.session.pushrules.ActionKt")
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun test() = runTest {
        // Given
        val firstActions = listOf(Action.Notify)
        val secondActions = listOf(Action.DoNotNotify)
        val rules = listOf(
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
        every { fakeSession.fakePushRuleService.getPushRules().getAllRules() } returns rules

        // When
        updatePushRulesIfNeededUseCase.execute(fakeSession)

        // Then
        coVerifySequence {
            fakeSession.fakePushRuleService.getPushRules()
            // first set
            fakeSession.fakePushRuleService.updatePushRuleActions(any(), RuleIds.RULE_ID_POLL_START_ONE_TO_ONE, true, firstActions)
            fakeSession.fakePushRuleService.updatePushRuleActions(any(), RuleIds.RULE_ID_POLL_START_ONE_TO_ONE_UNSTABLE, true, firstActions)
            fakeSession.fakePushRuleService.updatePushRuleActions(any(), RuleIds.RULE_ID_POLL_END_ONE_TO_ONE, true, firstActions)
            // second set
            fakeSession.fakePushRuleService.updatePushRuleActions(any(), RuleIds.RULE_ID_POLL_START, false, secondActions)
            fakeSession.fakePushRuleService.updatePushRuleActions(any(), RuleIds.RULE_ID_POLL_END, false, secondActions)
            fakeSession.fakePushRuleService.updatePushRuleActions(any(), RuleIds.RULE_ID_POLL_END_UNSTABLE, false, secondActions)
        }
    }

    private fun givenARuleId(ruleId: String, enabled: Boolean, actions: List<Action>): PushRule {
        val pushRule = mockk<PushRule> {
            every { this@mockk.ruleId } returns ruleId
            every { this@mockk.enabled } returns enabled
            every { this@mockk.actions } returns actions
            every { getActions() } returns actions
        }
        val ruleAndKind = mockk<PushRuleAndKind> {
            every { this@mockk.pushRule } returns pushRule
            every { kind } returns mockk()
        }

        every { fakeSession.fakePushRuleService.getPushRules().findDefaultRule(ruleId) } returns ruleAndKind

        return pushRule
    }
}
