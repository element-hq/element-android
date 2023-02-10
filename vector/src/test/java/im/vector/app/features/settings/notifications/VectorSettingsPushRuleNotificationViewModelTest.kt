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

package im.vector.app.features.settings.notifications

import com.airbnb.mvrx.test.MavericksTestRule
import im.vector.app.test.fakes.FakeActiveSessionHolder
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
import org.matrix.android.sdk.api.session.pushrules.rest.PushRuleAndKind

internal class VectorSettingsPushRuleNotificationViewModelTest {

    @get:Rule
    val mavericksTestRule = MavericksTestRule(testDispatcher = testDispatcher)

    private val fakeActiveSessionHolder = FakeActiveSessionHolder()
    private val fakePushRuleService = fakeActiveSessionHolder.fakeSession.fakePushRuleService

    private val initialState = VectorSettingsPushRuleNotificationViewState()
    private fun createViewModel() = VectorSettingsPushRuleNotificationViewModel(
            initialState = initialState,
            activeSessionHolder = fakeActiveSessionHolder.instance,
    )

    @Before
    fun setup() {
        mockkStatic("im.vector.app.features.settings.notifications.NotificationIndexKt")
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
        fakePushRuleService.givenUpdatePushRuleActionsSucceed()

        // When
        val viewModelTest = viewModel.test()
        viewModel.handle(VectorSettingsPushRuleNotificationViewAction.UpdatePushRule(givenARuleId(firstRuleId), true))
        viewModel.handle(VectorSettingsPushRuleNotificationViewAction.UpdatePushRule(givenARuleId(secondRuleId), false))

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
                        VectorSettingsPushRuleNotificationViewEvent.PushRuleUpdated(RuleIds.RULE_ID_ONE_TO_ONE_ROOM, true),
                        VectorSettingsPushRuleNotificationViewEvent.PushRuleUpdated(RuleIds.RULE_ID_ALL_OTHER_MESSAGES_ROOMS, false),
                )
                .finish()
    }

    @Test
    fun `given a ruleId, when the rule is checked with an error, then expected view event is posted`() = runTest {
        // Given
        val viewModel = createViewModel()
        val failure = mockk<Throwable>()

        val firstRuleId = RuleIds.RULE_ID_ONE_TO_ONE_ROOM
        fakePushRuleService.givenUpdatePushRuleActionsSucceed()
        fakePushRuleService.givenUpdatePushRuleActionsFail(RuleIds.RULE_ID_POLL_START_ONE_TO_ONE_UNSTABLE, failure)

        val secondRuleId = RuleIds.RULE_ID_ALL_OTHER_MESSAGES_ROOMS
        fakePushRuleService.givenUpdatePushRuleActionsFail(secondRuleId, failure)
        fakePushRuleService.givenUpdatePushRuleActionsFail(RuleIds.RULE_ID_POLL_START, failure)
        fakePushRuleService.givenUpdatePushRuleActionsFail(RuleIds.RULE_ID_POLL_START_UNSTABLE, failure)
        fakePushRuleService.givenUpdatePushRuleActionsFail(RuleIds.RULE_ID_POLL_END, failure)
        fakePushRuleService.givenUpdatePushRuleActionsFail(RuleIds.RULE_ID_POLL_END_UNSTABLE, failure)

        // When
        val viewModelTest = viewModel.test()
        // One rule failed to update
        viewModel.handle(VectorSettingsPushRuleNotificationViewAction.UpdatePushRule(givenARuleId(firstRuleId), true))
        // All the rules failed to update
        viewModel.handle(VectorSettingsPushRuleNotificationViewAction.UpdatePushRule(givenARuleId(secondRuleId), true))

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
                        VectorSettingsPushRuleNotificationViewEvent.PushRuleUpdated(RuleIds.RULE_ID_ONE_TO_ONE_ROOM, true, failure),
                        VectorSettingsPushRuleNotificationViewEvent.Failure(failure),
                )
                .finish()
    }

    @Test
    fun `given a ruleId, when the rule is unchecked with an error, then the expected view event is posted`() = runTest {
        // Given
        val viewModel = createViewModel()
        val failure = mockk<Throwable>()

        val firstRuleId = RuleIds.RULE_ID_ONE_TO_ONE_ROOM
        fakePushRuleService.givenUpdatePushRuleActionsSucceed()
        fakePushRuleService.givenUpdatePushRuleActionsFail(RuleIds.RULE_ID_POLL_START_ONE_TO_ONE_UNSTABLE, failure)

        val secondRuleId = RuleIds.RULE_ID_ALL_OTHER_MESSAGES_ROOMS
        fakePushRuleService.givenUpdatePushRuleActionsFail(secondRuleId, failure)
        fakePushRuleService.givenUpdatePushRuleActionsFail(RuleIds.RULE_ID_POLL_START, failure)
        fakePushRuleService.givenUpdatePushRuleActionsFail(RuleIds.RULE_ID_POLL_START_UNSTABLE, failure)
        fakePushRuleService.givenUpdatePushRuleActionsFail(RuleIds.RULE_ID_POLL_END, failure)
        fakePushRuleService.givenUpdatePushRuleActionsFail(RuleIds.RULE_ID_POLL_END_UNSTABLE, failure)

        // When
        val viewModelTest = viewModel.test()
        // One rule failed to update
        viewModel.handle(VectorSettingsPushRuleNotificationViewAction.UpdatePushRule(givenARuleId(firstRuleId), false))
        // All the rules failed to update
        viewModel.handle(VectorSettingsPushRuleNotificationViewAction.UpdatePushRule(givenARuleId(secondRuleId), false))

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
                        // The global rule remains checked if all the rules are not unchecked
                        VectorSettingsPushRuleNotificationViewEvent.PushRuleUpdated(RuleIds.RULE_ID_ONE_TO_ONE_ROOM, true, failure),
                        VectorSettingsPushRuleNotificationViewEvent.Failure(failure),
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

    private fun givenARuleId(ruleId: String, notificationIndex: NotificationIndex = NotificationIndex.NOISY): PushRuleAndKind {
        val ruleAndKind = mockk<PushRuleAndKind> {
            every { pushRule.ruleId } returns ruleId
            every { pushRule.notificationIndex } returns notificationIndex
            every { kind } returns mockk()
        }

        every { fakePushRuleService.getPushRules().findDefaultRule(ruleId) } returns ruleAndKind

        return ruleAndKind
    }
}
