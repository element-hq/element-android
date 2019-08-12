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

package im.vector.riotx.core.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap
import im.vector.riotx.core.platform.ConfigurationViewModel
import im.vector.riotx.features.crypto.keysbackup.restore.KeysBackupRestoreFromKeyViewModel
import im.vector.riotx.features.crypto.keysbackup.restore.KeysBackupRestoreFromPassphraseViewModel
import im.vector.riotx.features.crypto.keysbackup.restore.KeysBackupRestoreSharedViewModel
import im.vector.riotx.features.crypto.keysbackup.settings.KeysBackupSettingsViewModel
import im.vector.riotx.features.crypto.keysbackup.settings.KeysBackupSettingsViewModel_AssistedFactory
import im.vector.riotx.features.crypto.keysbackup.setup.KeysBackupSetupSharedViewModel
import im.vector.riotx.features.crypto.verification.SasVerificationViewModel
import im.vector.riotx.features.home.HomeActivityViewModel
import im.vector.riotx.features.home.HomeActivityViewModel_AssistedFactory
import im.vector.riotx.features.home.HomeDetailViewModel
import im.vector.riotx.features.home.HomeDetailViewModel_AssistedFactory
import im.vector.riotx.features.home.HomeNavigationViewModel
import im.vector.riotx.features.home.createdirect.CreateDirectRoomNavigationViewModel
import im.vector.riotx.features.home.createdirect.CreateDirectRoomViewModel
import im.vector.riotx.features.home.createdirect.CreateDirectRoomViewModel_AssistedFactory
import im.vector.riotx.features.home.group.GroupListViewModel
import im.vector.riotx.features.home.group.GroupListViewModel_AssistedFactory
import im.vector.riotx.features.home.room.detail.RoomDetailViewModel
import im.vector.riotx.features.home.room.detail.RoomDetailViewModel_AssistedFactory
import im.vector.riotx.features.home.room.detail.composer.TextComposerViewModel
import im.vector.riotx.features.home.room.detail.composer.TextComposerViewModel_AssistedFactory
import im.vector.riotx.features.home.room.detail.timeline.action.MessageActionsViewModel
import im.vector.riotx.features.home.room.detail.timeline.action.MessageActionsViewModel_AssistedFactory
import im.vector.riotx.features.home.room.detail.timeline.action.MessageMenuViewModel
import im.vector.riotx.features.home.room.detail.timeline.action.MessageMenuViewModel_AssistedFactory
import im.vector.riotx.features.home.room.detail.timeline.action.QuickReactionViewModel
import im.vector.riotx.features.home.room.detail.timeline.action.QuickReactionViewModel_AssistedFactory
import im.vector.riotx.features.home.room.detail.timeline.action.ViewEditHistoryViewModel
import im.vector.riotx.features.home.room.detail.timeline.action.ViewEditHistoryViewModel_AssistedFactory
import im.vector.riotx.features.home.room.detail.timeline.action.ViewReactionViewModel
import im.vector.riotx.features.home.room.detail.timeline.action.ViewReactionViewModel_AssistedFactory
import im.vector.riotx.features.home.room.list.RoomListViewModel
import im.vector.riotx.features.home.room.list.RoomListViewModel_AssistedFactory
import im.vector.riotx.features.reactions.EmojiChooserViewModel
import im.vector.riotx.features.roomdirectory.RoomDirectoryNavigationViewModel
import im.vector.riotx.features.roomdirectory.RoomDirectoryViewModel
import im.vector.riotx.features.roomdirectory.RoomDirectoryViewModel_AssistedFactory
import im.vector.riotx.features.roomdirectory.createroom.CreateRoomViewModel
import im.vector.riotx.features.roomdirectory.createroom.CreateRoomViewModel_AssistedFactory
import im.vector.riotx.features.roomdirectory.picker.RoomDirectoryPickerViewModel
import im.vector.riotx.features.roomdirectory.picker.RoomDirectoryPickerViewModel_AssistedFactory
import im.vector.riotx.features.roomdirectory.roompreview.RoomPreviewViewModel
import im.vector.riotx.features.roomdirectory.roompreview.RoomPreviewViewModel_AssistedFactory
import im.vector.riotx.features.settings.push.PushGatewaysViewModel
import im.vector.riotx.features.settings.push.PushGatewaysViewModel_AssistedFactory
import im.vector.riotx.features.workers.signout.SignOutViewModel

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
    @ViewModelKey(SignOutViewModel::class)
    fun bindSignOutViewModel(viewModel: SignOutViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(EmojiChooserViewModel::class)
    fun bindEmojiChooserViewModel(viewModel: EmojiChooserViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(SasVerificationViewModel::class)
    fun bindSasVerificationViewModel(viewModel: SasVerificationViewModel): ViewModel

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
    @ViewModelKey(RoomDirectoryNavigationViewModel::class)
    fun bindRoomDirectoryNavigationViewModel(viewModel: RoomDirectoryNavigationViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(HomeNavigationViewModel::class)
    fun bindHomeNavigationViewModel(viewModel: HomeNavigationViewModel): ViewModel

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
    @ViewModelKey(CreateDirectRoomNavigationViewModel::class)
    fun bindCreateDirectRoomNavigationViewModel(viewModel: CreateDirectRoomNavigationViewModel): ViewModel

    /**
     * Below are bindings for the MvRx view models (which extend VectorViewModel). Will be the only usage in the future.
     */

    @Binds
    fun bindHomeActivityViewModelFactory(factory: HomeActivityViewModel_AssistedFactory): HomeActivityViewModel.Factory

    @Binds
    fun bindTextComposerViewModelFactory(factory: TextComposerViewModel_AssistedFactory): TextComposerViewModel.Factory

    @Binds
    fun bindRoomDetailViewModelFactory(factory: RoomDetailViewModel_AssistedFactory): RoomDetailViewModel.Factory

    @Binds
    fun bindQuickReactionViewModelFactory(factory: QuickReactionViewModel_AssistedFactory): QuickReactionViewModel.Factory

    @Binds
    fun bindMessageActionsViewModelFactory(factory: MessageActionsViewModel_AssistedFactory): MessageActionsViewModel.Factory

    @Binds
    fun bindMessageMenuViewModelFactory(factory: MessageMenuViewModel_AssistedFactory): MessageMenuViewModel.Factory

    @Binds
    fun bindRoomListViewModelFactory(factory: RoomListViewModel_AssistedFactory): RoomListViewModel.Factory

    @Binds
    fun bindGroupListViewModelFactory(factory: GroupListViewModel_AssistedFactory): GroupListViewModel.Factory

    @Binds
    fun bindHomeDetailViewModelFactory(factory: HomeDetailViewModel_AssistedFactory): HomeDetailViewModel.Factory

    @Binds
    fun bindKeysBackupSettingsViewModelFactory(factory: KeysBackupSettingsViewModel_AssistedFactory): KeysBackupSettingsViewModel.Factory

    @Binds
    fun bindRoomDirectoryPickerViewModelFactory(factory: RoomDirectoryPickerViewModel_AssistedFactory): RoomDirectoryPickerViewModel.Factory

    @Binds
    fun bindRoomDirectoryViewModelFactory(factory: RoomDirectoryViewModel_AssistedFactory): RoomDirectoryViewModel.Factory

    @Binds
    fun bindRoomPreviewViewModelFactory(factory: RoomPreviewViewModel_AssistedFactory): RoomPreviewViewModel.Factory

    @Binds
    fun bindViewReactionViewModelFactory(factory: ViewReactionViewModel_AssistedFactory): ViewReactionViewModel.Factory

    @Binds
    fun bindViewEditHistoryViewModelFactory(factory: ViewEditHistoryViewModel_AssistedFactory): ViewEditHistoryViewModel.Factory

    @Binds
    fun bindCreateRoomViewModelFactory(factory: CreateRoomViewModel_AssistedFactory): CreateRoomViewModel.Factory

    @Binds
    fun bindCreateDirectRoomViewModelFactory(factory: CreateDirectRoomViewModel_AssistedFactory): CreateDirectRoomViewModel.Factory

    @Binds
    fun bindPushGatewaysViewModelFactory(factory: PushGatewaysViewModel_AssistedFactory): PushGatewaysViewModel.Factory

}