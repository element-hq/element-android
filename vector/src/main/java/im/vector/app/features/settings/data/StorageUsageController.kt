/*
 * Copyright (c) 2021 New Vector Ltd
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

package im.vector.app.features.settings.data

import com.airbnb.epoxy.TypedEpoxyController
import im.vector.app.core.ui.list.genericItem
import im.vector.app.core.utils.SizeByteFormatter
import javax.inject.Inject

class StorageUsageController @Inject constructor(
        private val sizeByteFormatter: SizeByteFormatter
) : TypedEpoxyController<StorageUsageViewState>() {

    override fun buildModels(data: StorageUsageViewState?) {
        data ?: return

        genericItem {
            id("media")
            title("Media and Files")
            description(
                    data.sizeOfMediaAndFiles.invoke()?.let {
                        this@StorageUsageController.sizeByteFormatter.formatFileSize(it)
                    } ?: "--"
            )
        }

        genericItem {
            id("logs")
            title("Log Files")
            description(
                    data.sizeOfLogs.invoke()?.let {
                        this@StorageUsageController.sizeByteFormatter.formatFileSize(it)
                    } ?: "--"
            )
        }

        genericItem {
            id("cache")
            title("Media cache")
            description(
                    data.sizeOfCache.invoke()?.let {
                        this@StorageUsageController.sizeByteFormatter.formatFileSize(it)
                    } ?: "--"
            )
        }

        genericItem {
            id("session")
            title("Session Database")
            description(
                    data.sizeOfSessionDatabase.invoke()?.let {
                        this@StorageUsageController.sizeByteFormatter.formatFileSize(it)
                    } ?: "--"
            )
        }

        genericItem {
            id("crypto_db")
            title("Crypto Database")
            description(
                    data.sizeOfCryptoDatabase.invoke()?.let {
                        this@StorageUsageController.sizeByteFormatter.formatFileSize(it)
                    } ?: "--"
            )
        }
    }
}
