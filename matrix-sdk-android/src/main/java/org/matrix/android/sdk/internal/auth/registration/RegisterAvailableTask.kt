/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2021 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.auth.registration

import org.matrix.android.sdk.api.auth.registration.RegistrationAvailability
import org.matrix.android.sdk.api.failure.Failure
import org.matrix.android.sdk.api.failure.isRegistrationAvailabilityError
import org.matrix.android.sdk.internal.auth.AuthAPI
import org.matrix.android.sdk.internal.network.executeRequest
import org.matrix.android.sdk.internal.task.Task

internal interface RegisterAvailableTask : Task<RegisterAvailableTask.Params, RegistrationAvailability> {
    data class Params(
            val userName: String
    )
}

internal class DefaultRegisterAvailableTask(private val authAPI: AuthAPI) : RegisterAvailableTask {
    override suspend fun execute(params: RegisterAvailableTask.Params): RegistrationAvailability {
        return try {
            executeRequest(null) {
                authAPI.registerAvailable(params.userName)
            }
            RegistrationAvailability.Available
        } catch (exception: Throwable) {
            if (exception.isRegistrationAvailabilityError()) {
                RegistrationAvailability.NotAvailable(exception as Failure.ServerError)
            } else {
                throw exception
            }
        }
    }
}
