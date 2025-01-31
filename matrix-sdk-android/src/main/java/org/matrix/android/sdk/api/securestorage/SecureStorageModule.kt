/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2022 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.securestorage

import android.content.Context
import dagger.Binds
import dagger.Module
import dagger.Provides
import org.matrix.android.sdk.api.util.BuildVersionSdkIntProvider
import org.matrix.android.sdk.api.util.DefaultBuildVersionSdkIntProvider
import java.security.KeyStore

@Module
internal abstract class SecureStorageModule {

    @Module
    companion object {
        @Provides
        fun provideKeyStore(): KeyStore = KeyStore.getInstance("AndroidKeyStore").also { it.load(null) }

        @Provides
        fun provideSecretStoringUtils(
                context: Context,
                keyStore: KeyStore,
                buildVersionSdkIntProvider: BuildVersionSdkIntProvider,
        ): SecretStoringUtils = SecretStoringUtils(context, keyStore, buildVersionSdkIntProvider)
    }

    @Binds
    abstract fun bindBuildVersionSdkIntProvider(provider: DefaultBuildVersionSdkIntProvider): BuildVersionSdkIntProvider
}
