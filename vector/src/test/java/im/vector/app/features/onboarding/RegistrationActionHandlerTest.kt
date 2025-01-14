/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.onboarding

import im.vector.app.test.fakes.FakeAuthenticationService
import im.vector.app.test.fakes.FakeRegistrationWizardActionDelegate
import im.vector.app.test.fakes.FakeSession
import im.vector.app.test.fakes.FakeStringProvider
import im.vector.app.test.fakes.FakeVectorFeatures
import im.vector.app.test.fakes.FakeVectorOverrides
import im.vector.app.test.fixtures.SelectedHomeserverStateFixture.aSelectedHomeserverState
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test
import org.matrix.android.sdk.api.auth.registration.FlowResult
import org.matrix.android.sdk.api.auth.registration.Stage

private val A_SESSION = FakeSession()

class RegistrationActionHandlerTest {

    private val fakeWizardActionDelegate = FakeRegistrationWizardActionDelegate()
    private val fakeAuthenticationService = FakeAuthenticationService()
    private val vectorOverrides = FakeVectorOverrides()
    private val vectorFeatures = FakeVectorFeatures()
    private val fakeStringProvider = FakeStringProvider().also {
        it.given(im.vector.app.config.R.string.matrix_org_server_url, "https://matrix.org")
    }

    private val registrationActionHandler = RegistrationActionHandler(
            fakeWizardActionDelegate.instance,
            fakeAuthenticationService,
            vectorOverrides,
            vectorFeatures,
            fakeStringProvider.instance
    )

    @Test
    fun `when processing SendAgainThreePid, then ignores result`() = runTest {
        val sendAgainThreePid = RegisterAction.SendAgainThreePid
        fakeWizardActionDelegate.givenResultsFor(listOf(sendAgainThreePid to RegistrationResult.Complete(A_SESSION)))

        val result = registrationActionHandler.processAction(sendAgainThreePid)

        result shouldBeEqualTo RegistrationActionHandler.Result.Ignored
    }

    @Test
    fun `given wizard delegate returns success, when handling action, then returns RegistrationComplete`() = runTest {
        fakeWizardActionDelegate.givenResultsFor(listOf(RegisterAction.StartRegistration to RegistrationResult.Complete(A_SESSION)))

        val result = registrationActionHandler.processAction(RegisterAction.StartRegistration)

        result shouldBeEqualTo RegistrationActionHandler.Result.RegistrationComplete(A_SESSION)
    }

    @Test
    fun `given flow result contains unsupported stages, when handling action, then returns UnsupportedStage`() = runTest {
        fakeAuthenticationService.givenRegistrationStarted(false)
        fakeWizardActionDelegate.givenResultsFor(listOf(RegisterAction.StartRegistration to anUnsupportedResult()))

        val result = registrationActionHandler.processAction(RegisterAction.StartRegistration)

        result shouldBeEqualTo RegistrationActionHandler.Result.UnsupportedStage
    }

    @Test
    fun `given flow result with mandatory and optional stages, when handling action, then returns mandatory stage`() = runTest {
        val mandatoryStage = Stage.ReCaptcha(mandatory = true, "ignored-key")
        val mixedStages = listOf(Stage.Email(mandatory = false), mandatoryStage)
        givenFlowResult(mixedStages)

        val result = registrationActionHandler.processAction(RegisterAction.StartRegistration)

        result shouldBeEqualTo RegistrationActionHandler.Result.NextStage(mandatoryStage)
    }

    @Test
    fun `given flow result with only optional stages, when handling action, then returns optional stage`() = runTest {
        val optionalStage = Stage.ReCaptcha(mandatory = false, "ignored-key")
        givenFlowResult(listOf(optionalStage))

        val result = registrationActionHandler.processAction(RegisterAction.StartRegistration)

        result shouldBeEqualTo RegistrationActionHandler.Result.NextStage(optionalStage)
    }

    @Test
    fun `given flow result with missing stages, when handling action, then returns MissingNextStage`() = runTest {
        givenFlowResult(emptyList())

        val result = registrationActionHandler.processAction(RegisterAction.StartRegistration)

        result shouldBeEqualTo RegistrationActionHandler.Result.MissingNextStage
    }

    @Test
    fun `given flow result with only optional dummy stage, when handling action, then returns MissingNextStage`() = runTest {
        givenFlowResult(listOf(Stage.Dummy(mandatory = false)))

        val result = registrationActionHandler.processAction(RegisterAction.StartRegistration)

        result shouldBeEqualTo RegistrationActionHandler.Result.MissingNextStage
    }

    @Test
    fun `given non matrix org homeserver and flow result with missing mandatory stages, when handling action, then returns first item`() = runTest {
        val firstStage = Stage.ReCaptcha(mandatory = true, "ignored-key")
        val orderedStages = listOf(firstStage, Stage.Email(mandatory = true), Stage.Msisdn(mandatory = true))
        givenFlowResult(orderedStages)

        val result = registrationActionHandler.processAction(RegisterAction.StartRegistration)

        result shouldBeEqualTo RegistrationActionHandler.Result.NextStage(firstStage)
    }

    @Test
    fun `given matrix org homeserver and flow result with missing mandatory stages, when handling action, then returns email item first`() = runTest {
        vectorFeatures.givenCombinedRegisterEnabled()
        val expectedFirstItem = Stage.Email(mandatory = true)
        val orderedStages = listOf(Stage.ReCaptcha(mandatory = true, "ignored-key"), expectedFirstItem, Stage.Msisdn(mandatory = true))
        givenFlowResult(orderedStages)

        val result = registrationActionHandler.processAction(state = aSelectedHomeserverState("https://matrix.org/"), RegisterAction.StartRegistration)

        result shouldBeEqualTo RegistrationActionHandler.Result.NextStage(expectedFirstItem)
    }

    @Test
    fun `given password already sent and missing mandatory dummy stage, when handling action, then fast tracks the dummy stage`() = runTest {
        val stages = listOf(Stage.ReCaptcha(mandatory = true, "ignored-key"), Stage.Email(mandatory = true), Stage.Dummy(mandatory = true))
        fakeAuthenticationService.givenRegistrationStarted(true)
        fakeWizardActionDelegate.givenResultsFor(
                listOf(
                        RegisterAction.StartRegistration to aFlowResult(stages),
                        RegisterAction.RegisterDummy to RegistrationResult.Complete(A_SESSION)
                )
        )

        val result = registrationActionHandler.processAction(RegisterAction.StartRegistration)

        result shouldBeEqualTo RegistrationActionHandler.Result.RegistrationComplete(A_SESSION)
    }

    private fun givenFlowResult(stages: List<Stage>) {
        fakeAuthenticationService.givenRegistrationStarted(true)
        fakeWizardActionDelegate.givenResultsFor(listOf(RegisterAction.StartRegistration to aFlowResult(stages)))
    }

    private fun aFlowResult(missingStages: List<Stage>) = RegistrationResult.NextStep(
            FlowResult(
                    missingStages = missingStages,
                    completedStages = emptyList()
            )
    )

    private fun anUnsupportedResult() = RegistrationResult.NextStep(
            FlowResult(
                    missingStages = listOf(Stage.Other(mandatory = true, "ignored-type", emptyMap<String, String>())),
                    completedStages = emptyList()
            )
    )

    private suspend fun RegistrationActionHandler.processAction(action: RegisterAction) = processAction(aSelectedHomeserverState(), action)
}
