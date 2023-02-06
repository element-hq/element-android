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

package im.vector.app.features.home

interface NightlyProxy {
    /**
     * Return true if this is a nightly build (checking the package of the app), and only once a day.
     */
    fun canDisplayPopup(): Boolean

    /**
     * Return true if this is a nightly build (checking the package of the app).
     */
    fun isNightlyBuild(): Boolean

    /**
     * Try to update the application, if update is available. Will also take care of the user sign in.
     */
    fun updateApplication()
}
