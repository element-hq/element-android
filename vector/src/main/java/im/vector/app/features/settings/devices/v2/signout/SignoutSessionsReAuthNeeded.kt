/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.settings.devices.v2.signout

import org.matrix.android.sdk.api.auth.UIABaseAuth
import org.matrix.android.sdk.api.auth.registration.RegistrationFlowResponse
import kotlin.coroutines.Continuation

data class SignoutSessionsReAuthNeeded(
        val pendingAuth: UIABaseAuth,
        val uiaContinuation: Continuation<UIABaseAuth>,
        val flowResponse: RegistrationFlowResponse,
        val errCode: String?
)
