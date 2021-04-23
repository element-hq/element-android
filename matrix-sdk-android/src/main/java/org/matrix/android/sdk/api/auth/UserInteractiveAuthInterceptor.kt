/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.api.auth

import org.matrix.android.sdk.api.auth.registration.RegistrationFlowResponse
import kotlin.coroutines.Continuation

/**
 * Some API endpoints require authentication that interacts with the user.
 * The homeserver may provide many different ways of authenticating, such as user/password auth, login via a social network (OAuth2),
 * login by confirming a token sent to their email address, etc.
 *
 * The process takes the form of one or more 'stages'.
 * At each stage the client submits a set of data for a given authentication type and awaits a response from the server,
 * which will either be a final success or a request to perform an additional stage.
 * This exchange continues until the final success.
 *
 * For each endpoint, a server offers one or more 'flows' that the client can use to authenticate itself.
 * Each flow comprises a series of stages, as described above.
 * The client is free to choose which flow it follows, however the flow's stages must be completed in order.
 * Failing to follow the flows in order must result in an HTTP 401 response.
 * When all stages in a flow are complete, authentication is complete and the API call succeeds.
 */
interface UserInteractiveAuthInterceptor {

    /**
     * When the API needs additional auth, this will be called.
     * Implementation should check the flows from flow response and act accordingly.
     * Updated auth should be provided using promise.resume, this allow implementation to perform
     * an async operation (prompt for user password, open sso fallback) and then resume initial API call when done.
     */
    fun performStage(flowResponse: RegistrationFlowResponse, errCode: String?, promise: Continuation<UIABaseAuth>)
}
