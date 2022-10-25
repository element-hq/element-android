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

package im.vector.app.test.fakes

import im.vector.app.features.settings.devices.v2.signout.InterceptSignoutFlowResponseUseCase
import im.vector.app.features.settings.devices.v2.signout.SignoutSessionResult
import im.vector.app.features.settings.devices.v2.signout.SignoutSessionUseCase
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.matrix.android.sdk.api.auth.UIABaseAuth
import org.matrix.android.sdk.api.auth.UserInteractiveAuthInterceptor
import org.matrix.android.sdk.api.auth.registration.RegistrationFlowResponse
import kotlin.coroutines.Continuation

class FakeSignoutSessionUseCase {

    val instance = mockk<SignoutSessionUseCase>()

    fun givenSignoutSuccess(
            deviceId: String,
            interceptSignoutFlowResponseUseCase: InterceptSignoutFlowResponseUseCase,
    ) {
        val interceptor = slot<UserInteractiveAuthInterceptor>()
        val flowResponse = mockk<RegistrationFlowResponse>()
        val errorCode = "errorCode"
        val promise = mockk<Continuation<UIABaseAuth>>()
        every { interceptSignoutFlowResponseUseCase.execute(flowResponse, errorCode, promise) } returns SignoutSessionResult.Completed
        coEvery { instance.execute(deviceId, capture(interceptor)) } coAnswers {
            secondArg<UserInteractiveAuthInterceptor>().performStage(flowResponse, errorCode, promise)
            Result.success(Unit)
        }
    }

    fun givenSignoutReAuthNeeded(
            deviceId: String,
            interceptSignoutFlowResponseUseCase: InterceptSignoutFlowResponseUseCase,
    ): SignoutSessionResult.ReAuthNeeded {
        val interceptor = slot<UserInteractiveAuthInterceptor>()
        val flowResponse = mockk<RegistrationFlowResponse>()
        every { flowResponse.session } returns "a-session-id"
        val errorCode = "errorCode"
        val promise = mockk<Continuation<UIABaseAuth>>()
        val reAuthNeeded = SignoutSessionResult.ReAuthNeeded(
                pendingAuth = mockk(),
                uiaContinuation = promise,
                flowResponse = flowResponse,
                errCode = errorCode,
        )
        every { interceptSignoutFlowResponseUseCase.execute(flowResponse, errorCode, promise) } returns reAuthNeeded
        coEvery { instance.execute(deviceId, capture(interceptor)) } coAnswers {
            secondArg<UserInteractiveAuthInterceptor>().performStage(flowResponse, errorCode, promise)
            Result.success(Unit)
        }

        return reAuthNeeded
    }

    fun givenSignoutError(deviceId: String, error: Throwable) {
        coEvery { instance.execute(deviceId, any()) } returns Result.failure(error)
    }
}
