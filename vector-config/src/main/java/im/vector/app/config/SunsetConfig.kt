/*
 * Copyright (c) 2025 New Vector Ltd
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

package im.vector.app.config

sealed interface SunsetConfig {
    /**
     * Sunsetting the application is disabled.
     */
    data object Disabled : SunsetConfig

    /**
     * Sunsetting the application is enabled and can be configured by implementing this class.
     */
    data class Enabled(
            /**
             * The URL target to learn more.
             */
            val learnMoreLink: String,

            /**
             * The replacement application ID.
             * Example: for Element application, the replacement application ID is the id of Element X: "Element X".
             */
            val replacementApplicationName: String,

            /**
             * The replacement application ID.
             * Example: for Element App, the replacement application ID is the id of Element X: "io.element.android.x".
             */
            val replacementApplicationId: String,
    ) : SunsetConfig
}
