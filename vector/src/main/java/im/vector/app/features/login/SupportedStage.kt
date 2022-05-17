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

package im.vector.app.features.login

import org.matrix.android.sdk.api.auth.registration.Stage

/**
 * Stage.Other is not supported, as well as any other new stages added to the SDK before it is added to the list below.
 */
fun Stage.isSupported(): Boolean {
    return this is Stage.ReCaptcha ||
            this is Stage.Dummy ||
            this is Stage.Msisdn ||
            this is Stage.Terms ||
            this is Stage.Email
}
