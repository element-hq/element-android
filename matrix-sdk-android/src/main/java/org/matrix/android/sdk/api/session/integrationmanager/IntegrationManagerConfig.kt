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
package org.matrix.android.sdk.api.session.integrationmanager

/**
 * This class holds configuration of integration manager.
 */
data class IntegrationManagerConfig(
        val uiUrl: String,
        val restUrl: String,
        val kind: Kind
) {

    // Order matters, first is preferred
    /**
     * The kind of config, it will reflect where the data is coming from.
     */
    enum class Kind {
        /**
         * Defined in UserAccountData
         */
        ACCOUNT,

        /**
         * Defined in Wellknown
         */
        HOMESERVER,

        /**
         * Fallback value, hardcoded by the SDK
         */
        DEFAULT
    }
}
