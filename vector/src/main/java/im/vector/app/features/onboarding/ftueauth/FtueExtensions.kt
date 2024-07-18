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

import im.vector.app.features.login.SignMode
import im.vector.app.features.onboarding.OnboardingAction
import im.vector.app.features.themes.ThemeProvider
import im.vector.lib.ui.styles.R

fun SignMode.toAuthenticateAction(login: String, password: String, initialDeviceName: String): OnboardingAction.AuthenticateAction {
    return when (this) {
        SignMode.Unknown -> error("developer error")
        SignMode.SignUp -> OnboardingAction.AuthenticateAction.Register(username = login, password, initialDeviceName)
        SignMode.SignIn -> OnboardingAction.AuthenticateAction.Login(username = login, password, initialDeviceName)
        SignMode.SignInWithMatrixId -> OnboardingAction.AuthenticateAction.LoginDirect(matrixId = login, password, initialDeviceName)
    }
}

fun ThemeProvider.ftueBreakerBackground() = when (isLightTheme()) {
    true -> R.drawable.bg_gradient_ftue_breaker
    false -> R.drawable.bg_color_background
}
