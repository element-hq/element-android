/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2022 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.failure

/**
 * This enum provide the reason why the SDK request an initial sync to the application.
 */
enum class InitialSyncRequestReason {
    /**
     * The list of ignored users has changed, and at least one user who was ignored is not ignored anymore.
     */
    IGNORED_USERS_LIST_CHANGE,
}
