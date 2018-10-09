/*
 * Copyright 2016 OpenMarket Ltd
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
package im.vector.matrix.android.internal.legacy.rest.model.login;

import java.util.List;
import java.util.Map;

/**
 * Response to a POST /register call with the different flows.
 */
public class RegistrationFlowResponse implements java.io.Serializable {
    /**
     * The list of stages the client has completed successfully.
     */
    public List<LoginFlow> flows;

    /**
     * The list of stages the client has completed successfully.
     */
    public List<String> completed;

    /**
     * The session identifier that the client must pass back to the home server, if one is provided,
     * in subsequent attempts to authenticate in the same API call.
     */
    public String session;

    /**
     * The information that the client will need to know in order to use a given type of authentication.
     * For each login stage type presented, that type may be present as a key in this dictionary.
     * For example, the public key of reCAPTCHA stage could be given here.
     */
    public Map<String, Object> params;
}
