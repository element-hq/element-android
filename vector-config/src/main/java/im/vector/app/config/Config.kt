/*
 * Copyright (c) 2022 New Vector Ltd
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

/**
 * Set of flags to configure the application.
 */
object Config {
    /**
     * Flag to allow external UnifiedPush distributors to be chosen by the user.
     *
     * Set to true to allow any available external UnifiedPush distributor to be chosen by the user.
     * - For Gplay variant it means that FCM will be used by default, but user can choose another UnifiedPush distributor;
     * - For F-Droid variant, it means that background polling will be used by default, but user can choose another UnifiedPush distributor.
     *
     * Set to false to prevent usage of external UnifiedPush distributors.
     * - For Gplay variant it means that only FCM will be used;
     * - For F-Droid variant, it means that only background polling will be available to the user.
     *
     * *Note*: Changing the value from `true` to `false` when the app is already installed on users' phone may have unexpected behavior.
     */
    const val ALLOW_EXTERNAL_UNIFIED_PUSH_DISTRIBUTORS = true
}
