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

package im.vector.app.features

import android.content.Context
import im.vector.app.R
import im.vector.app.features.VectorFeatures.LoginType

interface VectorFeatures {

    fun loginType(): LoginType

    enum class LoginType {
        V1,
        V2
    }
}

class DefaultVectorFeatures(private val context: Context) : VectorFeatures {
    override fun loginType(): LoginType {
        val v2LoginIsEnabled = context.resources.getBoolean(R.bool.useLoginV2)
        return if (v2LoginIsEnabled) {
            LoginType.V2
        } else {
            LoginType.V1
        }
    }
}
