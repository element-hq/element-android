/*
 * Copyright (c) 2023 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
