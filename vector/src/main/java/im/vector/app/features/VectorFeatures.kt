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

import im.vector.app.BuildConfig

interface VectorFeatures {

    fun loginVersion(): LoginVersion

    enum class LoginVersion {
        V1,
        V2
    }

    enum class NotificationSettingsVersion {
        V1,
        V2
    }
}

class DefaultVectorFeatures : VectorFeatures {
    override fun loginVersion(): VectorFeatures.LoginVersion = BuildConfig.LOGIN_VERSION
}
