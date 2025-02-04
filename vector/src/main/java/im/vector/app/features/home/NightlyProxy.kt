/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
