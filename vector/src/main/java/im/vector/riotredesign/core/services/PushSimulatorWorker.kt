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

package im.vector.riotredesign.core.services

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters

/**
 * This class simulate push event when FCM is not working/disabled
 */
class PushSimulatorWorker(val context: Context,
                          workerParams: WorkerParameters) : Worker(context, workerParams) {

    override fun doWork(): Result {
        // Simulate a Push
        EventStreamServiceX.onSimulatedPushReceived(context)

        // Indicate whether the task finished successfully with the Result
        return Result.success()
    }
}