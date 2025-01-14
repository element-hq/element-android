/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.analytics

import im.vector.app.features.analytics.errors.ErrorTracker
import kotlinx.coroutines.flow.Flow

interface VectorAnalytics : AnalyticsTracker, ErrorTracker {
    /**
     * Return a Flow of Boolean, true if the user has given their consent.
     */
    fun getUserConsent(): Flow<Boolean>

    /**
     * Update the user consent value.
     */
    suspend fun setUserConsent(userConsent: Boolean)

    /**
     * Return a Flow of Boolean, true if the user has been asked for their consent.
     */
    fun didAskUserConsent(): Flow<Boolean>

    /**
     * Store the fact that the user has been asked for their consent.
     */
    suspend fun setDidAskUserConsent()

    /**
     * Return a Flow of String, used for analytics Id.
     */
    fun getAnalyticsId(): Flow<String>

    /**
     * Update analyticsId from the AccountData.
     */
    suspend fun setAnalyticsId(analyticsId: String)

    /**
     * To be called when a session is destroyed.
     */
    suspend fun onSignOut()

    /**
     * To be called when application is started.
     */
    fun init()
}
