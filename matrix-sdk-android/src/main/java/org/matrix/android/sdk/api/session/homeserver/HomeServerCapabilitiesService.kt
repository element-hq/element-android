/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session.homeserver

/**
 * This interface defines a method to retrieve the homeserver capabilities.
 */
interface HomeServerCapabilitiesService {

    /**
     * Force a refresh of the stored data.
     */
    suspend fun refreshHomeServerCapabilities()

    /**
     * Get the HomeServer capabilities.
     */
    fun getHomeServerCapabilities(): HomeServerCapabilities
}
