/*
 * Copyright 2019 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.matrix.android.internal.auth.registration

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import im.vector.matrix.android.api.auth.registration.FlowResult
import im.vector.matrix.android.api.auth.registration.Stage
import im.vector.matrix.android.api.auth.registration.TermPolicies
import im.vector.matrix.android.api.util.JsonDict
import im.vector.matrix.android.internal.auth.data.InteractiveAuthenticationFlow
import im.vector.matrix.android.internal.auth.data.LoginFlowTypes

@JsonClass(generateAdapter = true)
data class RegistrationFlowResponse(

        /**
         * The list of flows.
         */
        @Json(name = "flows")
        var flows: List<InteractiveAuthenticationFlow>? = null,

        /**
         * The list of stages the client has completed successfully.
         */
        @Json(name = "completed")
        var completedStages: List<String>? = null,

        /**
         * The session identifier that the client must pass back to the home server, if one is provided,
         * in subsequent attempts to authenticate in the same API call.
         */
        @Json(name = "session")
        var session: String? = null,

        /**
         * The information that the client will need to know in order to use a given type of authentication.
         * For each login stage type presented, that type may be present as a key in this dictionary.
         * For example, the public key of reCAPTCHA stage could be given here.
         */
        @Json(name = "params")
        var params: JsonDict? = null,

        /**
         * The two MatrixError fields can also be present here in case of error when validating a stage
         */
        @Json(name = "errcode")
        var code: String? = null,

        @Json(name = "error")
        var message: String? = null
)

/**
 * Convert to something easier to exploit on client side
 */
fun RegistrationFlowResponse.toFlowResult(): FlowResult {
    // Get all the returned stages
    val allFlowTypes = mutableSetOf<String>()

    val missingStage = mutableListOf<Stage>()
    val completedStage = mutableListOf<Stage>()

    this.flows?.forEach { it.stages?.mapTo(allFlowTypes) { type -> type } }

    allFlowTypes.forEach { type ->
        val isMandatory = flows?.all { type in it.stages ?: emptyList() } == true

        val stage = when (type) {
            LoginFlowTypes.RECAPTCHA      -> Stage.ReCaptcha(isMandatory, ((params?.get(type) as? Map<*, *>)?.get("public_key") as? String)
                    ?: "")
            LoginFlowTypes.DUMMY          -> Stage.Dummy
            LoginFlowTypes.TERMS          -> Stage.Terms(isMandatory, TermPolicies())
            LoginFlowTypes.EMAIL_IDENTITY -> Stage.Email(isMandatory)
            LoginFlowTypes.MSISDN         -> Stage.Msisdn(isMandatory)
            else                          -> Stage.Other(isMandatory, type, (params?.get(type) as? Map<*, *>))
        }

        if (type in completedStages ?: emptyList()) {
            completedStage.add(stage)
        } else {
            missingStage.add(stage)
        }
    }

    return FlowResult(missingStage, completedStage)
}

