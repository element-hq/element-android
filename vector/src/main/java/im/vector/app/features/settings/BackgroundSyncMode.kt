/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.settings

/**
 * Different strategies for Background sync, only applicable to F-Droid version of the app.
 */
enum class BackgroundSyncMode {
    /**
     * In this mode background syncs are scheduled via Workers, meaning that the system will have control on the periodicity
     * of syncs when battery is low or when the phone is idle (sync will occur in allowed maintenance windows). After completion
     * the sync work will schedule another one.
     */
    FDROID_BACKGROUND_SYNC_MODE_FOR_BATTERY,

    /**
     * This mode requires the app to be exempted from battery optimization. Alarms will be launched and will wake up the app
     * in order to perform the background sync as a foreground service. After completion the service will schedule another alarm
     */
    FDROID_BACKGROUND_SYNC_MODE_FOR_REALTIME,

    /**
     * The app won't sync in background.
     */
    FDROID_BACKGROUND_SYNC_MODE_DISABLED;

    companion object {
        const val DEFAULT_SYNC_DELAY_SECONDS = 60
        const val DEFAULT_SYNC_TIMEOUT_SECONDS = 6

        fun fromString(value: String?): BackgroundSyncMode = values().firstOrNull { it.name == value }
                ?: FDROID_BACKGROUND_SYNC_MODE_DISABLED
    }
}
