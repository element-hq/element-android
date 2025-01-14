/*
 * Copyright 2023, 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.settings.notifications

import com.airbnb.mvrx.test.MavericksTestRule
import im.vector.app.features.settings.notifications.usecase.GetPushRulesOnInvalidStateUseCase
import im.vector.app.test.fakes.FakeSession
import im.vector.app.test.fixtures.PushRuleFixture
import im.vector.app.test.test
import im.vector.app.test.testDispatcher
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBe
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.matrix.android.sdk.api.session.pushrules.RuleIds

internal class VectorSettingsPushRuleNotificationViewModelTest {

    @get:Rule
    val mavericksTestRule = MavericksTestRule(testDispatcher = testDispatcher)

    private val fakeSession = FakeSession()
    private val fakePushRuleService = fakeSession.fakePushRuleService
    private val fakeGetPushRulesOnInvalidStateUseCase = mockk<GetPushRulesOnInvalidStateUseCase>()

    private val initialState = VectorSettingsPushRuleNotificationViewState()
    private fun createViewModel() = VectorSettingsPushRuleNotificationViewModel(
            initialState = initialState,
            session = fakeSession,
            fakeGetPushRulesOnInvalidStateUseCase,
    )

    @Before
    fun setup() {
        mockkStatic("im.vector.app.features.settings.notifications.NotificationIndexKt")
        every { fakeGetPushRulesOnInvalidStateUseCase.execute(any()) } returns emptyList()
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `given a ruleId, when the rule is checked or unchecked with no error, then the expected view event is posted`() = runTest {
        // Given
        val viewModel = createViewModel()

        val firstRuleId = RuleIds.RULE_ID_ONE_TO_ONE_ROOM
        val secondRuleId = RuleIds.RULE_ID_ALL_OTHER_MESSAGES_ROOMS
        givenARuleId(firstRuleId)
        givenARuleId(secondRuleId)
        fakePushRuleService.givenUpdatePushRuleActionsSucceed()

        // When
        val viewModelTest = viewModel.test()
        viewModel.handle(VectorSettingsPushRuleNotificationViewAction.UpdatePushRule(firstRuleId, true))
        viewModel.handle(VectorSettingsPushRuleNotificationViewAction.UpdatePushRule(secondRuleId, false))

        // Then
        coVerifyOrder {
            // first rule id
            fakePushRuleService.updatePushRuleActions(any(), firstRuleId, any(), any())
            fakePushRuleService.updatePushRuleActions(any(), RuleIds.RULE_ID_POLL_START_ONE_TO_ONE, any(), any())
            fakePushRuleService.updatePushRuleActions(any(), RuleIds.RULE_ID_POLL_START_ONE_TO_ONE_UNSTABLE, any(), any())
            fakePushRuleService.updatePushRuleActions(any(), RuleIds.RULE_ID_POLL_END_ONE_TO_ONE, any(), any())
            fakePushRuleService.updatePushRuleActions(any(), RuleIds.RULE_ID_POLL_END_ONE_TO_ONE_UNSTABLE, any(), any())

            // second rule id
            fakePushRuleService.updatePushRuleActions(any(), secondRuleId, any(), any())
            fakePushRuleService.updatePushRuleActions(any(), RuleIds.RULE_ID_POLL_START, any(), any())
            fakePushRuleService.updatePushRuleActions(any(), RuleIds.RULE_ID_POLL_START_UNSTABLE, any(), any())
            fakePushRuleService.updatePushRuleActions(any(), RuleIds.RULE_ID_POLL_END, any(), any())
            fakePushRuleService.updatePushRuleActions(any(), RuleIds.RULE_ID_POLL_END_UNSTABLE, any(), any())
        }

        viewModelTest
                .assertStatesChanges(
                        initialState,
                        { copy(isLoading = true) },
                        { copy(isLoading = false) },
                        { copy(isLoading = true) },
                        { copy(isLoading = false) },
                )
                .assertEvents(
                        VectorSettingsPushRuleNotificationViewEvent.PushRuleUpdated(firstRuleId, true),
                        VectorSettingsPushRuleNotificationViewEvent.PushRuleUpdated(secondRuleId, false),
                )
                .finish()
    }

    @Test
    fun `given a ruleId, when the rule is checked with an error, then expected view event is posted`() = runTest {
        // Given
        val viewModel = createViewModel()
        val failure = mockk<Throwable>()

        val firstRuleId = RuleIds.RULE_ID_ONE_TO_ONE_ROOM
        givenARuleId(firstRuleId)
        fakePushRuleService.givenUpdatePushRuleActionsSucceed()
        fakePushRuleService.givenUpdatePushRuleActionsFail(RuleIds.RULE_ID_POLL_START_ONE_TO_ONE_UNSTABLE, failure)

        val secondRuleId = RuleIds.RULE_ID_ALL_OTHER_MESSAGES_ROOMS
        givenARuleId(secondRuleId)
        fakePushRuleService.givenUpdatePushRuleActionsFail(secondRuleId, failure)
        fakePushRuleService.givenUpdatePushRuleActionsFail(RuleIds.RULE_ID_POLL_START, failure)
        fakePushRuleService.givenUpdatePushRuleActionsFail(RuleIds.RULE_ID_POLL_START_UNSTABLE, failure)
        fakePushRuleService.givenUpdatePushRuleActionsFail(RuleIds.RULE_ID_POLL_END, failure)
        fakePushRuleService.givenUpdatePushRuleActionsFail(RuleIds.RULE_ID_POLL_END_UNSTABLE, failure)

        // When
        val viewModelTest = viewModel.test()
        // One rule failed to update
        viewModel.handle(VectorSettingsPushRuleNotificationViewAction.UpdatePushRule(firstRuleId, true))
        // All the rules failed to update
        viewModel.handle(VectorSettingsPushRuleNotificationViewAction.UpdatePushRule(secondRuleId, true))

        // Then
        coVerifyOrder {
            // first rule id
            fakePushRuleService.updatePushRuleActions(any(), firstRuleId, any(), any())
            fakePushRuleService.updatePushRuleActions(any(), RuleIds.RULE_ID_POLL_START_ONE_TO_ONE, any(), any())
            fakePushRuleService.updatePushRuleActions(any(), RuleIds.RULE_ID_POLL_START_ONE_TO_ONE_UNSTABLE, any(), any())
            fakePushRuleService.updatePushRuleActions(any(), RuleIds.RULE_ID_POLL_END_ONE_TO_ONE, any(), any())
            fakePushRuleService.updatePushRuleActions(any(), RuleIds.RULE_ID_POLL_END_ONE_TO_ONE_UNSTABLE, any(), any())

            // second rule id
            fakePushRuleService.updatePushRuleActions(any(), secondRuleId, any(), any())
            fakePushRuleService.updatePushRuleActions(any(), RuleIds.RULE_ID_POLL_START, any(), any())
            fakePushRuleService.updatePushRuleActions(any(), RuleIds.RULE_ID_POLL_START_UNSTABLE, any(), any())
            fakePushRuleService.updatePushRuleActions(any(), RuleIds.RULE_ID_POLL_END, any(), any())
            fakePushRuleService.updatePushRuleActions(any(), RuleIds.RULE_ID_POLL_END_UNSTABLE, any(), any())
        }

        viewModelTest
                .assertStatesChanges(
                        initialState,
                        { copy(isLoading = true) },
                        { copy(isLoading = false, rulesOnError = setOf(firstRuleId)) },
                        { copy(isLoading = true) },
                        { copy(isLoading = false) },
                )
                .assertEvents(
                        VectorSettingsPushRuleNotificationViewEvent.PushRuleUpdated(firstRuleId, true, failure),
                        VectorSettingsPushRuleNotificationViewEvent.Failure(secondRuleId, failure),
                )
                .finish()
    }

    @Test
    fun `given a ruleId, when the rule is unchecked with an error, then the expected view event is posted`() = runTest {
        // Given
        val viewModel = createViewModel()
        val failure = mockk<Throwable>()

        val firstRuleId = RuleIds.RULE_ID_ONE_TO_ONE_ROOM
        givenARuleId(firstRuleId)
        fakePushRuleService.givenUpdatePushRuleActionsSucceed()
        fakePushRuleService.givenUpdatePushRuleActionsFail(RuleIds.RULE_ID_POLL_START_ONE_TO_ONE_UNSTABLE, failure)

        val secondRuleId = RuleIds.RULE_ID_ALL_OTHER_MESSAGES_ROOMS
        givenARuleId(secondRuleId)
        fakePushRuleService.givenUpdatePushRuleActionsFail(secondRuleId, failure)
        fakePushRuleService.givenUpdatePushRuleActionsFail(RuleIds.RULE_ID_POLL_START, failure)
        fakePushRuleService.givenUpdatePushRuleActionsFail(RuleIds.RULE_ID_POLL_START_UNSTABLE, failure)
        fakePushRuleService.givenUpdatePushRuleActionsFail(RuleIds.RULE_ID_POLL_END, failure)
        fakePushRuleService.givenUpdatePushRuleActionsFail(RuleIds.RULE_ID_POLL_END_UNSTABLE, failure)

        // When
        val viewModelTest = viewModel.test()
        // One rule failed to update
        viewModel.handle(VectorSettingsPushRuleNotificationViewAction.UpdatePushRule(firstRuleId, false))
        // All the rules failed to update
        viewModel.handle(VectorSettingsPushRuleNotificationViewAction.UpdatePushRule(secondRuleId, false))

        // Then
        coVerifyOrder {
            // first rule id
            fakePushRuleService.updatePushRuleActions(any(), firstRuleId, any(), any())
            fakePushRuleService.updatePushRuleActions(any(), RuleIds.RULE_ID_POLL_START_ONE_TO_ONE, any(), any())
            fakePushRuleService.updatePushRuleActions(any(), RuleIds.RULE_ID_POLL_START_ONE_TO_ONE_UNSTABLE, any(), any())
            fakePushRuleService.updatePushRuleActions(any(), RuleIds.RULE_ID_POLL_END_ONE_TO_ONE, any(), any())
            fakePushRuleService.updatePushRuleActions(any(), RuleIds.RULE_ID_POLL_END_ONE_TO_ONE_UNSTABLE, any(), any())

            // second rule id
            fakePushRuleService.updatePushRuleActions(any(), secondRuleId, any(), any())
            fakePushRuleService.updatePushRuleActions(any(), RuleIds.RULE_ID_POLL_START, any(), any())
            fakePushRuleService.updatePushRuleActions(any(), RuleIds.RULE_ID_POLL_START_UNSTABLE, any(), any())
            fakePushRuleService.updatePushRuleActions(any(), RuleIds.RULE_ID_POLL_END, any(), any())
            fakePushRuleService.updatePushRuleActions(any(), RuleIds.RULE_ID_POLL_END_UNSTABLE, any(), any())
        }

        viewModelTest
                .assertStatesChanges(
                        initialState,
                        { copy(isLoading = true) },
                        { copy(isLoading = false, rulesOnError = setOf(firstRuleId)) },
                        { copy(isLoading = true) },
                        { copy(isLoading = false) },
                )
                .assertEvents(
                        // The global rule remains checked if all the rules are not unchecked
                        VectorSettingsPushRuleNotificationViewEvent.PushRuleUpdated(firstRuleId, true, failure),
                        VectorSettingsPushRuleNotificationViewEvent.Failure(secondRuleId, failure),
                )
                .finish()
    }

    @Test
    fun `given a rule id, when requesting the check state, returns the expected value according to the related rules`() {
        // Given
        val viewModel = createViewModel()
        val firstRuleId = RuleIds.RULE_ID_ONE_TO_ONE_ROOM
        givenARuleId(firstRuleId, NotificationIndex.OFF)
        givenARuleId(RuleIds.RULE_ID_POLL_START_ONE_TO_ONE, NotificationIndex.OFF)
        givenARuleId(RuleIds.RULE_ID_POLL_START_ONE_TO_ONE_UNSTABLE, NotificationIndex.SILENT)
        givenARuleId(RuleIds.RULE_ID_POLL_END_ONE_TO_ONE, NotificationIndex.NOISY)
        givenARuleId(RuleIds.RULE_ID_POLL_END_ONE_TO_ONE_UNSTABLE, NotificationIndex.OFF)

        val secondRuleId = RuleIds.RULE_ID_ALL_OTHER_MESSAGES_ROOMS
        givenARuleId(secondRuleId, NotificationIndex.OFF)
        givenARuleId(RuleIds.RULE_ID_POLL_START, NotificationIndex.OFF)
        givenARuleId(RuleIds.RULE_ID_POLL_START_UNSTABLE, NotificationIndex.OFF)
        givenARuleId(RuleIds.RULE_ID_POLL_END, NotificationIndex.OFF)
        givenARuleId(RuleIds.RULE_ID_POLL_END_UNSTABLE, NotificationIndex.OFF)

        // When
        val firstResult = viewModel.isPushRuleChecked(firstRuleId)
        val secondResult = viewModel.isPushRuleChecked(secondRuleId)

        // Then
        firstResult shouldBe true
        secondResult shouldBe false
    }

    private fun givenARuleId(ruleId: String, notificationIndex: NotificationIndex = NotificationIndex.NOISY) {
        val pushRule = PushRuleFixture.aPushRule(ruleId)
        every { pushRule.notificationIndex } returns notificationIndex
        val ruleAndKind = PushRuleFixture.aPushRuleAndKind(pushRule)

        every { fakePushRuleService.getPushRules().findDefaultRule(ruleId) } returns ruleAndKind
    }
}
