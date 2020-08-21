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

package im.vector.app.features.discovery.change

import androidx.annotation.StringRes
import im.vector.app.core.platform.VectorViewEvents

sealed class SetIdentityServerViewEvents : VectorViewEvents {
    data class Loading(val message: CharSequence? = null) : SetIdentityServerViewEvents()
    data class Failure(@StringRes val errorMessageId: Int, val forDefault: Boolean) : SetIdentityServerViewEvents()
    data class OtherFailure(val failure: Throwable) : SetIdentityServerViewEvents()

    data class ShowTerms(val identityServerUrl: String) : SetIdentityServerViewEvents()

    object NoTerms : SetIdentityServerViewEvents()
    object TermsAccepted : SetIdentityServerViewEvents()
}
