/*
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
package im.vector.matrix.android.internal.legacy.rest.model;

/**
 * Response to a request an phone number validation request
 */
public class RequestPhoneNumberValidationResponse {

    // the client secret key
    public String clientSecret;

    // the attempt count
    public Integer sendAttempt;

    // the sid
    public String sid;

    // the msisdn
    public String msisdn;

    // phone number international format
    public String intl_fmt;
}
