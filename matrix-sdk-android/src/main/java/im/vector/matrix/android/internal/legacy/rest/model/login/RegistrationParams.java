/*
 * Copyright 2014 OpenMarket Ltd
 * Copyright 2017 Vector Creations Ltd
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

import java.util.Map;

/**
 * Class to pass parameters to the different registration types for /register.
 */
public class RegistrationParams {
    // authentification parameters
    public Map<String, Object> auth;

    // the account username
    public String username;

    // the account password
    public String password;

    // With email
    public Boolean bind_email;

    // With phone_number
    public Boolean bind_msisdn;

    // device name
    public String initial_device_display_name;

    // Temporary flag to notify the server that we support msisdn flow. Used to prevent old app
    // versions to end up in fallback because the HS returns the msisdn flow which they don't support
    public Boolean x_show_msisdn;
}
