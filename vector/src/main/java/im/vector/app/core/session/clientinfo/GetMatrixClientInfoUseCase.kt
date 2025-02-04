/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.core.session.clientinfo

import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.events.model.toModel
import javax.inject.Inject

/**
 * This use case retrieves the current account data event containing extended client info
 * for a given deviceId.
 */
class GetMatrixClientInfoUseCase @Inject constructor() {

    fun execute(session: Session, deviceId: String): MatrixClientInfoContent? {
        val type = MATRIX_CLIENT_INFO_KEY_PREFIX + deviceId
        val content = session.accountDataService().getUserAccountDataEvent(type)?.content
        return content.toModel()
    }
}
