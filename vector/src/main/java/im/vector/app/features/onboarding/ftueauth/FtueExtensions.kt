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

package im.vector.app.features.onboarding.ftueauth

import android.widget.Button
import com.google.android.material.textfield.TextInputLayout
import im.vector.app.core.extensions.hasContentFlow
import im.vector.app.features.login.SignMode
import im.vector.app.features.onboarding.OnboardingAction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onEach

fun SignMode.toAuthenticateAction(login: String, password: String, initialDeviceName: String): OnboardingAction.AuthenticateAction {
    return when (this) {
        SignMode.Unknown -> error("developer error")
        SignMode.SignUp -> OnboardingAction.AuthenticateAction.Register(username = login, password, initialDeviceName)
        SignMode.SignIn -> OnboardingAction.AuthenticateAction.Login(username = login, password, initialDeviceName)
        SignMode.SignInWithMatrixId -> OnboardingAction.AuthenticateAction.LoginDirect(matrixId = login, password, initialDeviceName)
    }
}

/**
 * A flow to monitor content changes from both username/id and password fields,
 * clearing errors and enabling/disabling the submission button on non empty content changes.
 */
fun observeContentChangesAndResetErrors(username: TextInputLayout, password: TextInputLayout, submit: Button): Flow<*> {
    return combine(
            username.hasContentFlow { it.trim() },
            password.hasContentFlow(),
            transform = { usernameHasContent, passwordHasContent -> usernameHasContent && passwordHasContent }
    ).onEach {
        username.error = null
        password.error = null
        submit.isEnabled = it
    }
}
