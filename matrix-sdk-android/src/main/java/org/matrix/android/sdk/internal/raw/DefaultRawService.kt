/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.raw

import org.matrix.android.sdk.api.MatrixCallback
import org.matrix.android.sdk.api.raw.RawCacheStrategy
import org.matrix.android.sdk.api.raw.RawService
import org.matrix.android.sdk.api.util.Cancelable
import org.matrix.android.sdk.internal.task.TaskExecutor
import org.matrix.android.sdk.internal.task.configureWith
import java.util.concurrent.TimeUnit
import javax.inject.Inject

internal class DefaultRawService @Inject constructor(
        private val taskExecutor: TaskExecutor,
        private val getUrlTask: GetUrlTask,
        private val cleanRawCacheTask: CleanRawCacheTask
) : RawService {
    override fun getUrl(url: String,
                        rawCacheStrategy: RawCacheStrategy,
                        matrixCallback: MatrixCallback<String>): Cancelable {
        return getUrlTask
                .configureWith(GetUrlTask.Params(url, rawCacheStrategy)) {
                    callback = matrixCallback
                }
                .executeBy(taskExecutor)
    }

    override fun getWellknown(userId: String,
                              matrixCallback: MatrixCallback<String>): Cancelable {
        val homeServerDomain = userId.substringAfter(":")
        return getUrl(
                "https://$homeServerDomain/.well-known/matrix/client",
                RawCacheStrategy.TtlCache(TimeUnit.HOURS.toMillis(8), false),
                matrixCallback
        )
    }

    override fun clearCache(matrixCallback: MatrixCallback<Unit>): Cancelable {
        return cleanRawCacheTask
                .configureWith(Unit) {
                    callback = matrixCallback
                }
                .executeBy(taskExecutor)
    }
}
