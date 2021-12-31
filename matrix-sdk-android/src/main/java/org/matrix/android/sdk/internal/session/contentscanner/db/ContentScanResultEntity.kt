/*
 * Copyright 2021 The Matrix.org Foundation C.I.C.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.matrix.android.sdk.internal.session.contentscanner.db

import io.realm.RealmObject
import io.realm.annotations.Index
import org.matrix.android.sdk.api.extensions.tryOrNull
import org.matrix.android.sdk.api.session.contentscanner.ScanState
import org.matrix.android.sdk.api.session.contentscanner.ScanStatusInfo

internal open class ContentScanResultEntity(
        @Index
        var mediaUrl: String? = null,
        var scanStatusString: String? = null,
        var humanReadableMessage: String? = null,
        var scanDateTimestamp: Long? = null,
        var scannerUrl: String? = null
) : RealmObject() {

    var scanResult: ScanState
        get() {
            return scanStatusString
                    ?.let {
                        tryOrNull { ScanState.valueOf(it) }
                    }
                    ?: ScanState.UNKNOWN
        }
        set(result) {
            scanStatusString = result.name
        }

    fun toModel(): ScanStatusInfo {
        return ScanStatusInfo(
                state = this.scanResult,
                humanReadableMessage = humanReadableMessage,
                scanDateTimestamp = scanDateTimestamp
        )
    }

    companion object
}
