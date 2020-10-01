/*
 * Copyright (c) 2020 New Vector Ltd
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

package org.matrix.android.sdk.api.util.profiling

import com.nikitakozlov.pury.Logger
import com.nikitakozlov.pury.Pury
import timber.log.Timber

object ProfilingConfiguration {

    init {
        Pury.setLogger(object : Logger {
            override fun result(tag: String, message: String) {
                Timber.tag(tag)
                Timber.v(message)
            }

            override fun warning(tag: String, message: String) {
                Timber.tag(tag)
                Timber.w(message)
            }

            override fun error(tag: String, message: String) {
                Timber.tag(tag)
                Timber.e(message)
            }
        })
    }
}
