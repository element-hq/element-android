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

package org.matrix.android.sdk.internal.worker

import androidx.work.OneTimeWorkRequest
import org.matrix.android.sdk.internal.session.room.send.NoMerger

/**
 * If startChain parameter is true, the builder will have a inputMerger set to [NoMerger].
 */
internal fun OneTimeWorkRequest.Builder.startChain(startChain: Boolean): OneTimeWorkRequest.Builder {
    if (startChain) {
        setInputMerger(NoMerger::class.java)
    }
    return this
}
