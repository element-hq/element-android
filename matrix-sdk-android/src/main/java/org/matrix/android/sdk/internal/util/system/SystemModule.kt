/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2021 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.util.system

import dagger.Binds
import dagger.Module
import org.matrix.android.sdk.api.securestorage.SecureStorageService
import org.matrix.android.sdk.internal.securestorage.DefaultSecureStorageService
import org.matrix.android.sdk.internal.util.time.Clock
import org.matrix.android.sdk.internal.util.time.DefaultClock

@Module
internal abstract class SystemModule {

    @Binds
    abstract fun bindSecureStorageService(service: DefaultSecureStorageService): SecureStorageService

    @Binds
    abstract fun bindClock(clock: DefaultClock): Clock
}
