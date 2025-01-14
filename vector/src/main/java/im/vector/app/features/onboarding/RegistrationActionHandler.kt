/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.onboarding

import im.vector.app.core.resources.StringProvider
import im.vector.app.core.utils.ensureTrailingSlash
import im.vector.app.features.VectorFeatures
import im.vector.app.features.VectorOverrides
import im.vector.app.features.login.isSupported
import im.vector.app.features.onboarding.ftueauth.MatrixOrgRegistrationStagesComparator
import kotlinx.coroutines.flow.first
import org.matrix.android.sdk.api.auth.AuthenticationService
import org.matrix.android.sdk.api.auth.registration.FlowResult
import org.matrix.android.sdk.api.auth.registration.RegisterThreePid
import org.matrix.android.sdk.api.auth.registration.Stage
import org.matrix.android.sdk.api.session.Session
import javax.inject.Inject

class RegistrationActionHandler @Inject constructor(
        private val registrationWizardActionDelegate: RegistrationWizardActionDelegate,
        private val authenticationService: AuthenticationService,
        private val vectorOverrides: VectorOverrides,
        private val vectorFeatures: VectorFeatures,
        stringProvider: StringProvider
) {

    private val matrixOrgUrl = stringProvider.getString(im.vector.app.config.R.string.matrix_org_server_url).ensureTrailingSlash()

    suspend fun processAction(state: SelectedHomeserverState, action: RegisterAction): Result {
        val result = registrationWizardActionDelegate.executeAction(action)
        return when {
            action.ignoresResult() -> Result.Ignored
            else -> when (result) {
                is RegistrationResult.Complete -> Result.RegistrationComplete(result.session)
                is RegistrationResult.NextStep -> processFlowResult(result, state)
                is RegistrationResult.SendEmailSuccess -> Result.SendEmailSuccess(result.email.email)
                is RegistrationResult.SendMsisdnSuccess -> Result.SendMsisdnSuccess(result.msisdn)
                is RegistrationResult.Error -> Result.Error(result.cause)
            }
        }
    }

    private suspend fun processFlowResult(result: RegistrationResult.NextStep, state: SelectedHomeserverState): Result {
        return if (shouldFastTrackDummyAction(result)) {
            processAction(state, RegisterAction.RegisterDummy)
        } else {
            handleNextStep(state, result.flowResult)
        }
    }

    private fun shouldFastTrackDummyAction(result: RegistrationResult.NextStep) = authenticationService.isRegistrationStarted() &&
            result.flowResult.missingStages.hasMandatoryDummy()

    private suspend fun handleNextStep(state: SelectedHomeserverState, flowResult: FlowResult): Result {
        return when {
            flowResult.registrationShouldFallback() -> Result.UnsupportedStage
            authenticationService.isRegistrationStarted() -> findNextStage(state, flowResult)
            else -> Result.StartRegistration
        }
    }

    private fun findNextStage(state: SelectedHomeserverState, flowResult: FlowResult): Result {
        val orderedResult = when {
            state.hasSelectedMatrixOrg() && vectorFeatures.isOnboardingCombinedRegisterEnabled() -> flowResult.copy(
                    missingStages = flowResult.missingStages.sortedWith(MatrixOrgRegistrationStagesComparator())
            )
            else -> flowResult
        }
        return orderedResult.findNextRegistrationStage()
                ?.let { Result.NextStage(it) }
                ?: Result.MissingNextStage
    }

    private fun FlowResult.findNextRegistrationStage() = missingStages.firstMandatoryOrNull() ?: missingStages.ignoreDummy().firstOptionalOrNull()

    private suspend fun FlowResult.registrationShouldFallback() = vectorOverrides.forceLoginFallback.first() || missingStages.any { !it.isSupported() }

    private fun SelectedHomeserverState.hasSelectedMatrixOrg() = userFacingUrl == matrixOrgUrl

    sealed interface Result {
        data class RegistrationComplete(val session: Session) : Result
        data class NextStage(val stage: Stage) : Result
        data class Error(val cause: Throwable) : Result
        data class SendEmailSuccess(val email: String) : Result
        data class SendMsisdnSuccess(val msisdn: RegisterThreePid.Msisdn) : Result
        object MissingNextStage : Result
        object StartRegistration : Result
        object UnsupportedStage : Result
        object Ignored : Result
    }
}

private fun List<Stage>.firstMandatoryOrNull() = firstOrNull { it.mandatory }
private fun List<Stage>.firstOptionalOrNull() = firstOrNull { !it.mandatory }
private fun List<Stage>.ignoreDummy() = filter { it !is Stage.Dummy }
private fun List<Stage>.hasMandatoryDummy() = any { it is Stage.Dummy && it.mandatory }
