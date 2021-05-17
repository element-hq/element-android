/*
 * Copyright (c) 2021 New Vector Ltd
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

package fr.gouv.tchap.core.utils

object TchapUtils {

    /**
     * Tells whether a homeserver name corresponds to an external server or not
     *
     * @param homeServerName
     * @return true if external
     */
    fun isExternalTchapServer(homeServerName: String) = homeServerName.startsWith("e.") || homeServerName.startsWith("agent.externe.")

    /**
     * Get name part of a display name by removing the domain part if any.
     * For example in case of "Jean Martin [Modernisation]", this will return "Jean Martin".
     *
     * @param displayName
     * @return displayName without domain (null if the provided display name is null).
     */
    fun getNameFromDisplayName(displayName: String): String {
        return displayName.split(DISPLAY_NAME_FIRST_DELIMITER)
                .first()
                .trim()
    }

    /**
     * Get the potential domain name from a display name.
     * For example in case of "Jean Martin [Modernisation]", this will return "Modernisation".
     *
     * @param displayName
     * @return displayName without name, empty string if no domain is available.
     */
    fun getDomainFromDisplayName(displayName: String): String {
        return displayName.split(DISPLAY_NAME_FIRST_DELIMITER)
                .elementAtOrNull(1)
                ?.split(DISPLAY_NAME_SECOND_DELIMITER)
                ?.first()
                ?.trim()
                ?: DEFAULT_EMPTY_STRING
    }

    private const val DISPLAY_NAME_FIRST_DELIMITER = "["
    private const val DISPLAY_NAME_SECOND_DELIMITER = "]"
    private const val DEFAULT_EMPTY_STRING = ""
}
