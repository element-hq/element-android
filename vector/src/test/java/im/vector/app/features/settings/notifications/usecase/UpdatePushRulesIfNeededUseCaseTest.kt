/*
 * Copyright 2023, 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.settings.notifications.usecase

import im.vector.app.test.fakes.FakeSession
import im.vector.app.test.fixtures.PushRuleFixture
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
import org.matrix.android.sdk.api.session.pushrules.rest.PushRule

internal class UpdatePushRulesIfNeededUseCaseTest {

    private val fakeSession = FakeSession()
    private val fakeGetPushRulesOnInvalidStateUseCase = mockk<GetPushRulesOnInvalidStateUseCase>()
    private val updatePushRulesIfNeededUseCase = UpdatePushRulesIfNeededUseCase(fakeGetPushRulesOnInvalidStateUseCase)

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
        val firstParentEnabled = true
        val firstParentActions = listOf(Action.Notify)
        val firstParent = givenARuleId(RuleIds.RULE_ID_ONE_TO_ONE_ROOM, firstParentEnabled, firstParentActions)
        val secondParentEnabled = false
        val secondParentActions = emptyList<Action>()
        val secondParent = givenARuleId(RuleIds.RULE_ID_ALL_OTHER_MESSAGES_ROOMS, secondParentEnabled, secondParentActions)
        val rulesOnError = listOf(
                // first set of related rules
                firstParent,
                givenARuleId(RuleIds.RULE_ID_POLL_START_ONE_TO_ONE, true, emptyList()), // diff
                givenARuleId(RuleIds.RULE_ID_POLL_START_ONE_TO_ONE_UNSTABLE, true, emptyList()), // diff
                givenARuleId(RuleIds.RULE_ID_POLL_END_ONE_TO_ONE, false, listOf(Action.Notify)), // diff
                // second set of related rules
                secondParent,
                givenARuleId(RuleIds.RULE_ID_POLL_START, true, listOf(Action.Notify)), // diff
                givenARuleId(RuleIds.RULE_ID_POLL_END, false, listOf(Action.Notify)), // diff
                givenARuleId(RuleIds.RULE_ID_POLL_END_UNSTABLE, true, listOf()), // diff
        )
        every { fakeGetPushRulesOnInvalidStateUseCase.execute(fakeSession) } returns rulesOnError
        every { fakeSession.fakePushRuleService.getPushRules().getAllRules() } returns rulesOnError

        // When
        updatePushRulesIfNeededUseCase.execute(fakeSession)

        // Then
        coVerifySequence {
            fakeSession.fakePushRuleService.getPushRules()
            // first set
            fakeSession.fakePushRuleService.updatePushRuleActions(any(), RuleIds.RULE_ID_POLL_START_ONE_TO_ONE, firstParentEnabled, firstParentActions)
            fakeSession.fakePushRuleService.updatePushRuleActions(any(), RuleIds.RULE_ID_POLL_START_ONE_TO_ONE_UNSTABLE, firstParentEnabled, firstParentActions)
            fakeSession.fakePushRuleService.updatePushRuleActions(any(), RuleIds.RULE_ID_POLL_END_ONE_TO_ONE, firstParentEnabled, firstParentActions)
            // second set
            fakeSession.fakePushRuleService.updatePushRuleActions(any(), RuleIds.RULE_ID_POLL_START, secondParentEnabled, secondParentActions)
            fakeSession.fakePushRuleService.updatePushRuleActions(any(), RuleIds.RULE_ID_POLL_END, secondParentEnabled, secondParentActions)
            fakeSession.fakePushRuleService.updatePushRuleActions(any(), RuleIds.RULE_ID_POLL_END_UNSTABLE, secondParentEnabled, secondParentActions)
        }
    }

    private fun givenARuleId(ruleId: String, enabled: Boolean, actions: List<Action>): PushRule {
        val ruleAndKind = PushRuleFixture.aPushRuleAndKind(
                pushRule = PushRuleFixture.aPushRule(ruleId, enabled, actions),
        )

        every { fakeSession.fakePushRuleService.getPushRules().findDefaultRule(ruleId) } returns ruleAndKind

        return ruleAndKind.pushRule
    }
}
