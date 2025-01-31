/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.crypto.tasks

import org.matrix.android.sdk.api.auth.UIABaseAuth
import org.matrix.android.sdk.api.auth.UserInteractiveAuthInterceptor
import org.matrix.android.sdk.api.session.uia.UiaResult
import org.matrix.android.sdk.internal.auth.registration.handleUIA
import org.matrix.android.sdk.internal.crypto.api.CryptoApi
import org.matrix.android.sdk.internal.crypto.model.rest.DeleteDeviceParams
import org.matrix.android.sdk.internal.network.GlobalErrorReceiver
import org.matrix.android.sdk.internal.network.executeRequest
import org.matrix.android.sdk.internal.task.Task
import timber.log.Timber
import javax.inject.Inject

internal interface DeleteDeviceTask : Task<DeleteDeviceTask.Params, Unit> {
    data class Params(
            val deviceId: String,
            val userInteractiveAuthInterceptor: UserInteractiveAuthInterceptor?,
            val userAuthParam: UIABaseAuth?
    )
}

internal class DefaultDeleteDeviceTask @Inject constructor(
        private val cryptoApi: CryptoApi,
        private val globalErrorReceiver: GlobalErrorReceiver
) : DeleteDeviceTask {

    override suspend fun execute(params: DeleteDeviceTask.Params) {
        try {
            executeRequest(globalErrorReceiver) {
                cryptoApi.deleteDevice(params.deviceId, DeleteDeviceParams(params.userAuthParam?.asMap()))
            }
        } catch (throwable: Throwable) {
            if (params.userInteractiveAuthInterceptor == null ||
                    handleUIA(
                            failure = throwable,
                            interceptor = params.userInteractiveAuthInterceptor,
                            retryBlock = { authUpdate ->
                                execute(params.copy(userAuthParam = authUpdate))
                            }
                    ) != UiaResult.SUCCESS
            ) {
                Timber.d("## UIA: propagate failure")
                throw throwable
            }
        }
    }
}
