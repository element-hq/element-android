/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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

class InterceptSignoutFlowResponseUseCase @Inject constructor(
        private val reAuthHelper: ReAuthHelper,
        private val activeSessionHolder: ActiveSessionHolder,
) {

    fun execute(
            flowResponse: RegistrationFlowResponse,
            errCode: String?,
            promise: Continuation<UIABaseAuth>
    ): SignoutSessionsReAuthNeeded? {
        return if (flowResponse.nextUncompletedStage() == LoginFlowTypes.PASSWORD && reAuthHelper.data != null && errCode == null) {
            UserPasswordAuth(
                    session = null,
                    user = activeSessionHolder.getActiveSession().myUserId,
                    password = reAuthHelper.data
            ).let { promise.resume(it) }
            null
        } else {
            SignoutSessionsReAuthNeeded(
                    pendingAuth = DefaultBaseAuth(session = flowResponse.session),
                    uiaContinuation = promise,
                    flowResponse = flowResponse,
                    errCode = errCode
            )
        }
    }
}
