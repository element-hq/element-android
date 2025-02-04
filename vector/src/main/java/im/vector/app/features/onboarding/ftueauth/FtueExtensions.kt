/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
