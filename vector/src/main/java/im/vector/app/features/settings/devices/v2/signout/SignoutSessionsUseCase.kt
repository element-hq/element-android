/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.settings.devices.v2.signout

import androidx.annotation.Size
import im.vector.app.core.di.ActiveSessionHolder
import org.matrix.android.sdk.api.auth.UIABaseAuth
import org.matrix.android.sdk.api.auth.UserInteractiveAuthInterceptor
import org.matrix.android.sdk.api.auth.registration.RegistrationFlowResponse
import timber.log.Timber
import javax.inject.Inject
import kotlin.coroutines.Continuation

class SignoutSessionsUseCase @Inject constructor(
        private val activeSessionHolder: ActiveSessionHolder,
        private val interceptSignoutFlowResponseUseCase: InterceptSignoutFlowResponseUseCase,
) {

    suspend fun execute(
            @Size(min = 1) deviceIds: List<String>,
            onReAuthNeeded: (SignoutSessionsReAuthNeeded) -> Unit,
    ): Result<Unit> = runCatching {
        Timber.d("start execute with ${deviceIds.size} deviceIds")

        val authInterceptor = object : UserInteractiveAuthInterceptor {
            override fun performStage(flowResponse: RegistrationFlowResponse, errCode: String?, promise: Continuation<UIABaseAuth>) {
                val result = interceptSignoutFlowResponseUseCase.execute(flowResponse, errCode, promise)
                result?.let(onReAuthNeeded)
            }
        }

        deleteDevices(deviceIds, authInterceptor)
        Timber.d("end execute")
    }

    private suspend fun deleteDevices(deviceIds: List<String>, userInteractiveAuthInterceptor: UserInteractiveAuthInterceptor) =
            activeSessionHolder.getActiveSession()
                    .cryptoService()
                    .deleteDevices(deviceIds, userInteractiveAuthInterceptor)
}
