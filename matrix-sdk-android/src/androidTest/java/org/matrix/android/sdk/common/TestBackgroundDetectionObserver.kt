/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2021 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.common

import org.matrix.android.sdk.internal.util.BackgroundDetectionObserver

/**
 * Force foreground for testing
 */
internal class TestBackgroundDetectionObserver : BackgroundDetectionObserver {

    override val isInBackground: Boolean = false

    override fun register(listener: BackgroundDetectionObserver.Listener) = Unit

    override fun unregister(listener: BackgroundDetectionObserver.Listener) = Unit
}
