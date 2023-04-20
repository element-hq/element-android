/*
 * Copyright (c) 2022 New Vector Ltd
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
