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

package org.matrix.android.sdk.api.session.widgets

interface WidgetURLFormatter {
    /**
     * Takes care of fetching a scalar token if required and build the final url.
     * This methods can throw, you should take care of handling failure.
     *
     * @param baseUrl the baseUrl which will be checked for scalar token
     * @param params additional params you want to append to the base url.
     * @param forceFetchScalarToken if true, you will force to fetch a new scalar token
     * from the server (only if the base url is whitelisted)
     * @param bypassWhitelist if true, the base url will be considered as whitelisted
     */
    suspend fun format(
            baseUrl: String,
            params: Map<String, String> = emptyMap(),
            forceFetchScalarToken: Boolean = false,
            bypassWhitelist: Boolean
    ): String
}
