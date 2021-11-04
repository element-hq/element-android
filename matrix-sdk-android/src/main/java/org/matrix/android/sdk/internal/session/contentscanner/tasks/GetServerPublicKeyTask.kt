/*
 * Copyright 2020 New Vector Ltd - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package org.matrix.android.sdk.internal.session.contentscanner.tasks

import org.matrix.android.sdk.internal.network.executeRequest
import org.matrix.android.sdk.internal.session.contentscanner.ContentScannerApi
import org.matrix.android.sdk.internal.session.contentscanner.model.ServerPublicKeyResponse
import org.matrix.android.sdk.internal.task.Task
import javax.inject.Inject

internal interface GetServerPublicKeyTask : Task<GetServerPublicKeyTask.Params, String?> {
    data class Params(
            val contentScannerApi: ContentScannerApi
    )
}

internal class DefaultGetServerPublicKeyTask @Inject constructor() : GetServerPublicKeyTask {

    override suspend fun execute(params: GetServerPublicKeyTask.Params): String? {
        return executeRequest<ServerPublicKeyResponse>(null) {
            params.contentScannerApi.getServerPublicKey()
        }.publicKey
    }
}
