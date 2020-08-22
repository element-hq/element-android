/*
 * Copyright 2019 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.app.core.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap
import im.vector.app.core.platform.ConfigurationViewModel
import im.vector.app.features.call.SharedActiveCallViewModel
import im.vector.app.features.crypto.keysbackup.restore.KeysBackupRestoreFromKeyViewModel
import im.vector.app.features.crypto.keysbackup.restore.KeysBackupRestoreFromPassphraseViewModel
import im.vector.app.features.crypto.keysbackup.restore.KeysBackupRestoreSharedViewModel
import im.vector.app.features.crypto.keysbackup.setup.KeysBackupSetupSharedViewModel
import im.vector.app.features.discovery.DiscoverySharedViewModel
import im.vector.app.features.home.HomeSharedActionViewModel
import im.vector.app.features.home.room.detail.RoomDetailSharedActionViewModel
import im.vector.app.features.home.room.detail.timeline.action.MessageSharedActionViewModel
import im.vector.app.features.home.room.list.actions.RoomListQuickActionsSharedActionViewModel
import im.vector.app.features.reactions.EmojiChooserViewModel
import im.vector.app.features.roomdirectory.RoomDirectorySharedActionViewModel
import im.vector.app.features.roomprofile.RoomProfileSharedActionViewModel
import im.vector.app.features.userdirectory.UserDirectorySharedActionViewModel

@Module
interface ViewModelModule {

    /**
     * ViewModels with @IntoMap will be injected by this factory
     */
    @Binds
    fun bindViewModelFactory(factory: VectorViewModelFactory): ViewModelProvider.Factory

    /**
     *  Below are bindings for the androidx view models (which extend ViewModel). Will be converted to MvRx ViewModel in the future.
     */

    @Binds
    @IntoMap
    @ViewModelKey(EmojiChooserViewModel::class)
    fun bindEmojiChooserViewModel(viewModel: EmojiChooserViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(KeysBackupRestoreFromKeyViewModel::class)
    fun bindKeysBackupRestoreFromKeyViewModel(viewModel: KeysBackupRestoreFromKeyViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(KeysBackupRestoreSharedViewModel::class)
    fun bindKeysBackupRestoreSharedViewModel(viewModel: KeysBackupRestoreSharedViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(KeysBackupRestoreFromPassphraseViewModel::class)
    fun bindKeysBackupRestoreFromPassphraseViewModel(viewModel: KeysBackupRestoreFromPassphraseViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(KeysBackupSetupSharedViewModel::class)
    fun bindKeysBackupSetupSharedViewModel(viewModel: KeysBackupSetupSharedViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(ConfigurationViewModel::class)
    fun bindConfigurationViewModel(viewModel: ConfigurationViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(SharedActiveCallViewModel::class)
    fun bindSharedActiveCallViewModel(viewModel: SharedActiveCallViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(UserDirectorySharedActionViewModel::class)
    fun bindUserDirectorySharedActionViewModel(viewModel: UserDirectorySharedActionViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(HomeSharedActionViewModel::class)
    fun bindHomeSharedActionViewModel(viewModel: HomeSharedActionViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(MessageSharedActionViewModel::class)
    fun bindMessageSharedActionViewModel(viewModel: MessageSharedActionViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(RoomListQuickActionsSharedActionViewModel::class)
    fun bindRoomListQuickActionsSharedActionViewModel(viewModel: RoomListQuickActionsSharedActionViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(RoomDirectorySharedActionViewModel::class)
    fun bindRoomDirectorySharedActionViewModel(viewModel: RoomDirectorySharedActionViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(RoomDetailSharedActionViewModel::class)
    fun bindRoomDetailSharedActionViewModel(viewModel: RoomDetailSharedActionViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(RoomProfileSharedActionViewModel::class)
    fun bindRoomProfileSharedActionViewModel(viewModel: RoomProfileSharedActionViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(DiscoverySharedViewModel::class)
    fun bindDiscoverySharedViewModel(viewModel: DiscoverySharedViewModel): ViewModel
}
