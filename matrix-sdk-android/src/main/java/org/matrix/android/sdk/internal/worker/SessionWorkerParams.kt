/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.worker

/**
 * Note about the Worker usage:
 * The workers we chain, or when using the append strategy, should never return Result.Failure(), else the chain will be broken forever.
 */
internal interface SessionWorkerParams {
    val sessionId: String

    /**
     * Null when no error occurs. When chaining Workers, first step is to check that there is no lastFailureMessage from the previous workers
     * If it is the case, the worker should just transmit the error and shouldn't do anything else.
     */
    val lastFailureMessage: String?
}
