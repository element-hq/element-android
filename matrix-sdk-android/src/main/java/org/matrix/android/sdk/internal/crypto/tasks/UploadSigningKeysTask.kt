/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.crypto.tasks

import org.matrix.android.sdk.api.auth.UIABaseAuth
import org.matrix.android.sdk.api.failure.Failure
import org.matrix.android.sdk.api.session.crypto.crosssigning.CryptoCrossSigningKey
import org.matrix.android.sdk.api.session.crypto.crosssigning.toRest
import org.matrix.android.sdk.internal.crypto.api.CryptoApi
import org.matrix.android.sdk.internal.crypto.model.rest.UploadSigningKeysBody
import org.matrix.android.sdk.internal.network.GlobalErrorReceiver
import org.matrix.android.sdk.internal.network.executeRequest
import org.matrix.android.sdk.internal.task.Task
import javax.inject.Inject

internal interface UploadSigningKeysTask : Task<UploadSigningKeysTask.Params, Unit> {
    data class Params(
            // the MSK
            val masterKey: CryptoCrossSigningKey,
            // the USK
            val userKey: CryptoCrossSigningKey,
            // the SSK
            val selfSignedKey: CryptoCrossSigningKey,
            /**
             * Authorisation info (User Interactive flow).
             */
            val userAuthParam: UIABaseAuth?
    )
}

internal data class UploadSigningKeys(val failures: Map<String, Any>?) : Failure.FeatureFailure()

internal class DefaultUploadSigningKeysTask @Inject constructor(
        private val cryptoApi: CryptoApi,
        private val globalErrorReceiver: GlobalErrorReceiver
) : UploadSigningKeysTask {

    override suspend fun execute(params: UploadSigningKeysTask.Params) {
        val uploadQuery = UploadSigningKeysBody(
                masterKey = params.masterKey.toRest(),
                userSigningKey = params.userKey.toRest(),
                selfSigningKey = params.selfSignedKey.toRest(),
                auth = params.userAuthParam?.asMap()
        )
        doRequest(uploadQuery)
    }

    private suspend fun doRequest(uploadQuery: UploadSigningKeysBody) {
        val keysQueryResponse = executeRequest(globalErrorReceiver) {
            cryptoApi.uploadSigningKeys(uploadQuery)
        }
        if (keysQueryResponse.failures?.isNotEmpty() == true) {
            throw UploadSigningKeys(keysQueryResponse.failures)
        }
    }
}
