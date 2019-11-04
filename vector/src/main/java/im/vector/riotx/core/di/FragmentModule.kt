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
 *
 */

package im.vector.riotx.core.di

import androidx.fragment.app.FragmentFactory
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap
import im.vector.riotx.core.platform.ConfigurationViewModel
import im.vector.riotx.features.crypto.keysbackup.restore.KeysBackupRestoreFromKeyViewModel
import im.vector.riotx.features.crypto.keysbackup.restore.KeysBackupRestoreFromPassphraseViewModel
import im.vector.riotx.features.crypto.keysbackup.restore.KeysBackupRestoreSharedViewModel
import im.vector.riotx.features.crypto.keysbackup.setup.KeysBackupSetupSharedViewModel
import im.vector.riotx.features.crypto.verification.SasVerificationViewModel
import im.vector.riotx.features.home.HomeNavigationViewModel
import im.vector.riotx.features.home.createdirect.CreateDirectRoomNavigationViewModel
import im.vector.riotx.features.home.room.list.RoomListFragment
import im.vector.riotx.features.reactions.EmojiChooserViewModel
import im.vector.riotx.features.roomdirectory.RoomDirectoryNavigationViewModel
import im.vector.riotx.features.workers.signout.SignOutViewModel

@Module
interface FragmentModule {

    /**
     * Fragments with @IntoMap will be injected by this factory
     */
    @Binds
    fun bindFragmentFactory(factory: VectorFragmentFactory): FragmentFactory


    @Binds
    @IntoMap
    @FragmentKey(RoomListFragment::class)
    fun bindRoomListFragment(viewModel: RoomListFragment): ViewModel


}
