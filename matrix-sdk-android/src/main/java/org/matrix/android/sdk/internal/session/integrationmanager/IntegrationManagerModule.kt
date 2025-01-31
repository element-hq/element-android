/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.integrationmanager

import dagger.Binds
import dagger.Module
import org.matrix.android.sdk.api.session.integrationmanager.IntegrationManagerService

@Module
internal abstract class IntegrationManagerModule {

    @Binds
    abstract fun bindIntegrationManagerService(service: DefaultIntegrationManagerService): IntegrationManagerService
}
