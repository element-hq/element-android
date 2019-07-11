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

package im.vector.riotx.core.error

import im.vector.matrix.android.api.failure.Failure
import im.vector.riotx.R
import im.vector.riotx.core.resources.StringProvider
import javax.inject.Inject

class ErrorFormatter @Inject constructor(val stringProvider: StringProvider) {


    fun toHumanReadable(failure: Failure): String {
        // Default
        return failure.localizedMessage
    }

    fun toHumanReadable(throwable: Throwable?): String {

        return when (throwable) {
            null                         -> ""
            is Failure.NetworkConnection -> stringProvider.getString(R.string.error_no_network)
            else                         -> throwable.localizedMessage
        }

    }
}