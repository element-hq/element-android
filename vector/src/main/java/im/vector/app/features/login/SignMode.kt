/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.login

enum class SignMode {
    Unknown,

    // Account creation
    SignUp,

    // Login
    SignIn,

    // Login directly with matrix Id
    SignInWithMatrixId
}
