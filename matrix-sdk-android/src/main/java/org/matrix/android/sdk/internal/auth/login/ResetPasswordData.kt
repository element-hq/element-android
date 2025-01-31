/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.auth.login

import com.squareup.moshi.JsonClass
import org.matrix.android.sdk.internal.auth.registration.AddThreePidRegistrationResponse

/**
 * Container to store the data when a reset password is in the email validation step.
 */
@JsonClass(generateAdapter = true)
internal data class ResetPasswordData(
        val addThreePidRegistrationResponse: AddThreePidRegistrationResponse
)
