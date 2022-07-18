/*
 * Copyright (c) 2022 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.app.features.pin.lockscreen.di

import android.content.Context
import androidx.biometric.BiometricManager
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import im.vector.app.core.di.MavericksAssistedViewModelFactory
import im.vector.app.core.di.MavericksViewModelKey
import im.vector.app.features.pin.PinCodeStore
import im.vector.app.features.pin.lockscreen.configuration.LockScreenConfiguration
import im.vector.app.features.pin.lockscreen.configuration.LockScreenMode
import im.vector.app.features.pin.lockscreen.crypto.KeyStoreCrypto
import im.vector.app.features.pin.lockscreen.crypto.LockScreenKeyRepository
import im.vector.app.features.pin.lockscreen.crypto.PinCodeMigrator
import im.vector.app.features.pin.lockscreen.pincode.EncryptedPinCodeStorage
import im.vector.app.features.pin.lockscreen.ui.LockScreenViewModel
import im.vector.app.features.settings.VectorPreferences
import org.matrix.android.sdk.api.securestorage.SecretStoringUtils
import org.matrix.android.sdk.api.util.BuildVersionSdkIntProvider
import org.matrix.android.sdk.api.util.DefaultBuildVersionSdkIntProvider
import java.security.KeyStore
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object LockScreenModule {

    @Provides
    fun provideKeyStore(): KeyStore = KeyStore.getInstance("AndroidKeyStore").also { it.load(null) }

    @Provides
    fun provideBuildVersionSdkIntProvider(): BuildVersionSdkIntProvider = DefaultBuildVersionSdkIntProvider()

    @Provides
    fun provideSecretStoringUtils(
            @ApplicationContext context: Context,
            keyStore: KeyStore,
            buildVersionSdkIntProvider: BuildVersionSdkIntProvider,
    ) = SecretStoringUtils(context, keyStore, buildVersionSdkIntProvider)

    @Provides
    fun provideLockScreenConfig() = LockScreenConfiguration(
            mode = LockScreenMode.VERIFY,
            pinCodeLength = 4,
            isWeakBiometricsEnabled = false,
            isDeviceCredentialUnlockEnabled = false,
            isStrongBiometricsEnabled = true,
            needsNewCodeValidation = true,
    )

    @Provides
    @Singleton
    fun provideKeyRepository(
            pinCodeMigrator: PinCodeMigrator,
            vectorPreferences: VectorPreferences,
            keyStoreCryptoFactory: KeyStoreCrypto.Factory,
    ) = LockScreenKeyRepository(
            baseName = "vector",
            pinCodeMigrator,
            vectorPreferences,
            keyStoreCryptoFactory,
    )

    @Provides
    fun provideBiometricManager(@ApplicationContext context: Context) = BiometricManager.from(context)
}

@Module
@InstallIn(SingletonComponent::class)
interface LockScreenBindsModule {

    @Binds
    @IntoMap
    @MavericksViewModelKey(LockScreenViewModel::class)
    fun bindLockScreenViewModel(factory: LockScreenViewModel.Factory): MavericksAssistedViewModelFactory<*, *>

    @Binds
    fun bindSharedPreferencesStorage(pinCodeStore: PinCodeStore): EncryptedPinCodeStorage
}
