/*
 * Copyright 2023, 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.mdm

import android.content.Context
import timber.log.Timber

enum class MdmData(val key: String) {
    DefaultHomeserverUrl(key = "im.vector.app.serverConfigDefaultHomeserverUrlString"),
    DefaultPushGatewayUrl(key = "im.vector.app.serverConfigSygnalAPIUrlString"),
    PermalinkBaseUrl(key = "im.vector.app.clientPermalinkBaseUrl"),
}

interface MdmService {
    fun registerListener(context: Context, onChangedListener: () -> Unit)
    fun unregisterListener(context: Context)
    fun getData(mdmData: MdmData): String?
    fun getData(mdmData: MdmData, defaultValue: String): String {
        return getData(mdmData)
                ?.also {
                    Timber.w("Using MDM data for ${mdmData.name}: $it")
                }
                ?: defaultValue
    }
}
