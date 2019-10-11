/*
 * Copyright 2019 New Vector Ltd
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

package im.vector.matrix.android.internal.worker

import android.content.Context
import androidx.work.*

// TODO Multiaccount
internal object WorkManagerUtil {
    private const val MATRIX_SDK_TAG = "MatrixSDK"

    /**
     * Default constraints: connected network
     */
    val workConstraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

    /**
     * Create a OneTimeWorkRequestBuilder, with the Matrix SDK tag
     */
    inline fun <reified W : ListenableWorker> matrixOneTimeWorkRequestBuilder() =
            OneTimeWorkRequestBuilder<W>()
                    .addTag(MATRIX_SDK_TAG)

    /**
     * Cancel all works instantiated by the Matrix SDK and not those from the SDK client
     */
    fun cancelAllWorks(context: Context) {
        WorkManager.getInstance(context).also {
            it.cancelAllWorkByTag(MATRIX_SDK_TAG)
            it.pruneWork()
        }
    }
}
