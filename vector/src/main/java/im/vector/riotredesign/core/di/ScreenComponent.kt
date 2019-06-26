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

package im.vector.riotredesign.core.di

import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import dagger.BindsInstance
import dagger.Component
import im.vector.fragments.keysbackup.restore.KeysBackupRestoreFromPassphraseFragment
import im.vector.matrix.android.api.session.Session
import im.vector.riotredesign.features.MainActivity
import im.vector.riotredesign.features.crypto.keysbackup.restore.KeysBackupRestoreFromKeyFragment
import im.vector.riotredesign.features.crypto.keysbackup.restore.KeysBackupRestoreSuccessFragment
import im.vector.riotredesign.features.crypto.keysbackup.settings.KeysBackupManageActivity
import im.vector.riotredesign.features.crypto.keysbackup.settings.KeysBackupSettingsFragment
import im.vector.riotredesign.features.crypto.keysbackup.setup.KeysBackupSetupStep1Fragment
import im.vector.riotredesign.features.crypto.keysbackup.setup.KeysBackupSetupStep2Fragment
import im.vector.riotredesign.features.crypto.keysbackup.setup.KeysBackupSetupStep3Fragment
import im.vector.riotredesign.features.crypto.verification.SASVerificationIncomingFragment
import im.vector.riotredesign.features.home.*
import im.vector.riotredesign.features.home.group.GroupListFragment
import im.vector.riotredesign.features.home.room.detail.RoomDetailFragment
import im.vector.riotredesign.features.home.room.detail.timeline.action.MessageActionsBottomSheet
import im.vector.riotredesign.features.home.room.detail.timeline.action.MessageMenuFragment
import im.vector.riotredesign.features.home.room.detail.timeline.action.QuickReactionFragment
import im.vector.riotredesign.features.home.room.detail.timeline.action.ViewReactionBottomSheet
import im.vector.riotredesign.features.home.room.list.RoomListFragment
import im.vector.riotredesign.features.invite.VectorInviteView
import im.vector.riotredesign.features.login.LoginActivity
import im.vector.riotredesign.features.media.ImageMediaViewerActivity
import im.vector.riotredesign.features.media.VideoMediaViewerActivity
import im.vector.riotredesign.features.rageshake.BugReportActivity
import im.vector.riotredesign.features.rageshake.BugReporter
import im.vector.riotredesign.features.rageshake.RageShake
import im.vector.riotredesign.features.reactions.EmojiReactionPickerActivity
import im.vector.riotredesign.features.roomdirectory.PublicRoomsFragment
import im.vector.riotredesign.features.roomdirectory.RoomDirectoryActivity
import im.vector.riotredesign.features.roomdirectory.createroom.CreateRoomFragment
import im.vector.riotredesign.features.roomdirectory.picker.RoomDirectoryPickerFragment
import im.vector.riotredesign.features.roomdirectory.roompreview.RoomPreviewNoPreviewFragment
import im.vector.riotredesign.features.settings.VectorSettingsActivity
import im.vector.riotredesign.features.settings.VectorSettingsPreferencesFragment

@Component(dependencies = [VectorComponent::class], modules = [ViewModelModule::class, HomeModule::class])
@ScreenScope
interface ScreenComponent {

    fun session(): Session

    fun viewModelFactory(): ViewModelProvider.Factory

    fun bugReporter(): BugReporter

    fun rageShake(): RageShake

    fun inject(activity: HomeActivity)

    fun inject(roomDetailFragment: RoomDetailFragment)

    fun inject(roomListFragment: RoomListFragment)

    fun inject(groupListFragment: GroupListFragment)

    fun inject(roomDirectoryPickerFragment: RoomDirectoryPickerFragment)

    fun inject(roomPreviewNoPreviewFragment: RoomPreviewNoPreviewFragment)

    fun inject(keysBackupSettingsFragment: KeysBackupSettingsFragment)

    fun inject(homeDrawerFragment: HomeDrawerFragment)

    fun inject(homeDetailFragment: HomeDetailFragment)

    fun inject(messageActionsBottomSheet: MessageActionsBottomSheet)

    fun inject(viewReactionBottomSheet: ViewReactionBottomSheet)

    fun inject(messageMenuFragment: MessageMenuFragment)

    fun inject(vectorSettingsActivity: VectorSettingsActivity)

    fun inject(createRoomFragment: CreateRoomFragment)

    fun inject(keysBackupManageActivity: KeysBackupManageActivity)

    fun inject(keysBackupRestoreFromKeyFragment: KeysBackupRestoreFromKeyFragment)

    fun inject(keysBackupRestoreFromPassphraseFragment: KeysBackupRestoreFromPassphraseFragment)

    fun inject(keysBackupRestoreSuccessFragment: KeysBackupRestoreSuccessFragment)

    fun inject(keysBackupSetupStep1Fragment: KeysBackupSetupStep1Fragment)

    fun inject(keysBackupSetupStep2Fragment: KeysBackupSetupStep2Fragment)

    fun inject(keysBackupSetupStep3Fragment: KeysBackupSetupStep3Fragment)

    fun inject(publicRoomsFragment: PublicRoomsFragment)

    fun inject(sasVerificationIncomingFragment: SASVerificationIncomingFragment)

    fun inject(quickReactionFragment: QuickReactionFragment)

    fun inject(emojiReactionPickerActivity: EmojiReactionPickerActivity)

    fun inject(loginActivity: LoginActivity)

    fun inject(mainActivity: MainActivity)

    fun inject(vectorSettingsPreferencesFragment: VectorSettingsPreferencesFragment)

    fun inject(roomDirectoryActivity: RoomDirectoryActivity)

    fun inject(bugReportActivity: BugReportActivity)

    fun inject(imageMediaViewerActivity: ImageMediaViewerActivity)

    fun inject(vectorInviteView: VectorInviteView)

    fun inject(videoMediaViewerActivity: VideoMediaViewerActivity)


    @Component.Factory
    interface Factory {
        fun create(vectorComponent: VectorComponent,
                   @BindsInstance context: AppCompatActivity
        ): ScreenComponent
    }
}
