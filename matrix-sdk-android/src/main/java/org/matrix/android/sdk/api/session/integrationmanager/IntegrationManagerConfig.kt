/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
         * Defined in UserAccountData.
         */
        ACCOUNT,

        /**
         * Defined in Wellknown.
         */
        HOMESERVER,

        /**
         * Fallback value, hardcoded by the SDK.
         */
        DEFAULT
    }
}
