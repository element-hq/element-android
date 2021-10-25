/*
 * Copyright 2020 New Vector Ltd - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package im.vector.matrix.android.sdk.internal.session.contentscanning.tasks

import im.vector.matrix.android.sdk.internal.session.contentscanning.ContentScanApi
import im.vector.matrix.android.sdk.internal.session.contentscanning.model.ServerPublicKeyResponse
import org.matrix.android.sdk.internal.network.executeRequest
import org.matrix.android.sdk.internal.task.Task

import javax.inject.Inject

internal interface GetServerPublicKeyTask : Task<GetServerPublicKeyTask.Params, String?> {
    data class Params(
            val contentScanApi: ContentScanApi
    )
}

internal class DefaultGetServerPublicKeyTask @Inject constructor() : GetServerPublicKeyTask {

    override suspend fun execute(params: GetServerPublicKeyTask.Params): String? {
        return executeRequest<ServerPublicKeyResponse>(null) {
            params.contentScanApi.getServerPublicKey()
        }.publicKey
    }
}
