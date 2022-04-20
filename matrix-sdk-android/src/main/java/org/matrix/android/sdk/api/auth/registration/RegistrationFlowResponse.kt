/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.api.auth.registration

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.matrix.android.sdk.api.auth.data.LoginFlowTypes
import org.matrix.android.sdk.api.session.uia.InteractiveAuthenticationFlow
import org.matrix.android.sdk.api.util.JsonDict

@JsonClass(generateAdapter = true)
data class RegistrationFlowResponse(

        /**
         * The list of flows.
         */
        @Json(name = "flows")
        val flows: List<InteractiveAuthenticationFlow>? = null,

        /**
         * The list of stages the client has completed successfully.
         */
        @Json(name = "completed")
        val completedStages: List<String>? = null,

        /**
         * The session identifier that the client must pass back to the homeserver, if one is provided,
         * in subsequent attempts to authenticate in the same API call.
         */
        @Json(name = "session")
        val session: String? = null,

        /**
         * The information that the client will need to know in order to use a given type of authentication.
         * For each login stage type presented, that type may be present as a key in this dictionary.
         * For example, the public key of reCAPTCHA stage could be given here.
         * other example
         *  "params": {
         *       "m.login.sso": {
         *          "identity_providers": [
         *              {
         *                  "id": "google",
         *                  "name": "Google",
         *                  "icon": "https://..."
         *              }
         *          ]
         *      }
         *  }
         */
        @Json(name = "params")
        val params: JsonDict? = null

        /**
         * WARNING,
         * The two MatrixError fields "errcode" and "error" can also be present here in case of error when validating a stage,
         * But in this case Moshi will be able to parse the result as a MatrixError, see [RetrofitExtensions.toFailure]
         * Ex: when polling for "m.login.msisdn" validation
         */
)

/**
 * Convert to something easier to handle on client side
 */
fun RegistrationFlowResponse.toFlowResult(): FlowResult {
    // Get all the returned stages
    val allFlowTypes = mutableSetOf<String>()

    val missingStage = mutableListOf<Stage>()
    val completedStage = mutableListOf<Stage>()

    this.flows?.forEach { it.stages?.mapTo(allFlowTypes) { type -> type } }

    allFlowTypes.forEach { type ->
        val isMandatory = flows?.all { type in it.stages.orEmpty() } == true

        val stage = when (type) {
            LoginFlowTypes.RECAPTCHA      -> Stage.ReCaptcha(isMandatory, ((params?.get(type) as? Map<*, *>)?.get("public_key") as? String)
                    ?: "")
            LoginFlowTypes.DUMMY          -> Stage.Dummy(isMandatory)
            LoginFlowTypes.TERMS          -> Stage.Terms(isMandatory, params?.get(type) as? TermPolicies ?: emptyMap<String, String>())
            LoginFlowTypes.EMAIL_IDENTITY -> Stage.Email(isMandatory)
            LoginFlowTypes.MSISDN         -> Stage.Msisdn(isMandatory)
            else                          -> Stage.Other(isMandatory, type, (params?.get(type) as? Map<*, *>))
        }

        if (type in completedStages.orEmpty()) {
            completedStage.add(stage)
        } else {
            missingStage.add(stage)
        }
    }

    return FlowResult(missingStage, completedStage)
}

fun RegistrationFlowResponse.nextUncompletedStage(flowIndex: Int = 0): String? {
    val completed = completedStages ?: emptyList()
    return flows?.getOrNull(flowIndex)?.stages?.firstOrNull { completed.contains(it).not() }
}
