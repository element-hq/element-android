/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.settings.threepids

import im.vector.app.core.platform.VectorViewEvents
import org.matrix.android.sdk.api.auth.registration.RegistrationFlowResponse

sealed class ThreePidsSettingsViewEvents : VectorViewEvents {
    data class Failure(val throwable: Throwable) : ThreePidsSettingsViewEvents()

    //    object RequestPassword : ThreePidsSettingsViewEvents()
    data class RequestReAuth(val registrationFlowResponse: RegistrationFlowResponse, val lastErrorCode: String?) : ThreePidsSettingsViewEvents()
}
