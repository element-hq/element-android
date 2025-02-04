/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.settings.devices.v2.details

import im.vector.app.core.session.clientinfo.MatrixClientInfoContent
import org.matrix.android.sdk.api.extensions.orFalse
import javax.inject.Inject

class CheckIfSectionApplicationIsVisibleUseCase @Inject constructor() {

    fun execute(matrixClientInfoContent: MatrixClientInfoContent?): Boolean {
        return matrixClientInfoContent?.name?.isNotEmpty().orFalse() ||
                matrixClientInfoContent?.version?.isNotEmpty().orFalse() ||
                matrixClientInfoContent?.url?.isNotEmpty().orFalse()
    }
}
