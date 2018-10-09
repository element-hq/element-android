/*
 * Copyright 2014 OpenMarket Ltd
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
package im.vector.matrix.android.internal.legacy.rest.model;

/**
 * The forget password params
 */
public class ForgetPasswordParams {

    /**
     * The email address
     **/
    public String email;

    /**
     * Client-generated secret string used to protect this session
     **/
    public String client_secret;

    /**
     * Used to distinguish protocol level retries from requests to re-send the email.
     **/
    public Integer send_attempt;

    /**
     * The ID server to send the onward request to as a hostname with an appended colon and port number if the port is not the default.
     **/
    public String id_server;
}
