/*
 * Copyright (c) 2022 New Vector Ltd
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

package im.vector.app.flipper

import okhttp3.Interceptor
import org.matrix.android.sdk.api.Matrix
import javax.inject.Inject

/**
 * No op version.
 */
@Suppress("UNUSED_PARAMETER")
class FlipperProxy @Inject constructor() {
    fun init(matrix: Matrix) {}

    fun getNetworkInterceptor(): Interceptor? = null
}
