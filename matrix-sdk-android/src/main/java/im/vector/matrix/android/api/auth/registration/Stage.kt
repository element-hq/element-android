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

package im.vector.matrix.android.api.auth.registration

import im.vector.matrix.android.api.util.JsonDict

sealed class Stage(open val mandatory: Boolean) {

    // m.login.password
    data class Password(override val mandatory: Boolean, val publicKey: String) : Stage(mandatory)

    // m.login.recaptcha
    data class ReCaptcha(override val mandatory: Boolean, val publicKey: String) : Stage(mandatory)

    // m.login.oauth2
    // m.login.email.identity
    data class Email(override val mandatory: Boolean, val policies: TermPolicies) : Stage(mandatory)

    // m.login.msisdn
    data class Msisdn(override val mandatory: Boolean, val policies: TermPolicies) : Stage(mandatory)
    // m.login.token
    // m.login.dummy

    // Undocumented yet: m.login.terms
    data class Terms(override val mandatory: Boolean, val policies: TermPolicies) : Stage(mandatory)

    // TODO SSO

    // For unknown stages
    data class Other(override val mandatory: Boolean, val type: String, val params: JsonDict?) : Stage(mandatory)
}

class TermPolicies
