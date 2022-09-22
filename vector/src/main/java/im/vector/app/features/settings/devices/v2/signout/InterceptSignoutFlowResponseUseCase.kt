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

import im.vector.app.core.di.ActiveSessionHolder
import im.vector.app.features.login.ReAuthHelper
import org.matrix.android.sdk.api.auth.UIABaseAuth
import org.matrix.android.sdk.api.auth.UserPasswordAuth
import org.matrix.android.sdk.api.auth.data.LoginFlowTypes
import org.matrix.android.sdk.api.auth.registration.RegistrationFlowResponse
import org.matrix.android.sdk.api.auth.registration.nextUncompletedStage
import org.matrix.android.sdk.api.session.uia.DefaultBaseAuth
import javax.inject.Inject
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume

// TODO add unit tests
class InterceptSignoutFlowResponseUseCase @Inject constructor(
        private val reAuthHelper: ReAuthHelper,
        private val activeSessionHolder: ActiveSessionHolder,
) {

    fun execute(
            flowResponse: RegistrationFlowResponse,
            errCode: String?,
            promise: Continuation<UIABaseAuth>
    ): SignoutSessionResult {
        return if (flowResponse.nextUncompletedStage() == LoginFlowTypes.PASSWORD && reAuthHelper.data != null && errCode == null) {
            UserPasswordAuth(
                    session = null,
                    user = activeSessionHolder.getActiveSession().myUserId,
                    password = reAuthHelper.data
            ).let { promise.resume(it) }

            SignoutSessionResult.Completed
        } else {
            SignoutSessionResult.ReAuthNeeded(
                    pendingAuth = DefaultBaseAuth(session = flowResponse.session),
                    uiaContinuation = promise,
                    flowResponse = flowResponse,
                    errCode = errCode
            )
        }
    }
}
