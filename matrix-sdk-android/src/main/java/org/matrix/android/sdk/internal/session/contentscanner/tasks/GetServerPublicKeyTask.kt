/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2021 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
