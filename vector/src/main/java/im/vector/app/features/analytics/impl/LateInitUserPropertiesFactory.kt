/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.analytics.impl

import android.content.Context
import im.vector.app.ActiveSessionDataSource
import im.vector.app.core.extensions.vectorStore
import im.vector.app.features.analytics.extensions.toTrackingValue
import im.vector.app.features.analytics.plan.UserProperties
import javax.inject.Inject

class LateInitUserPropertiesFactory @Inject constructor(
        private val activeSessionDataSource: ActiveSessionDataSource,
        private val context: Context,
) {
    suspend fun createUserProperties(): UserProperties? {
        val useCase = activeSessionDataSource.currentValue?.orNull()?.vectorStore(context)?.readUseCase()
        return useCase?.let {
            UserProperties(ftueUseCaseSelection = it.toTrackingValue())
        }
    }
}
