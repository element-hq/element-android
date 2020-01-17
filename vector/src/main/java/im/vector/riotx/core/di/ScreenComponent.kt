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

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentFactory
import androidx.lifecycle.ViewModelProvider
import dagger.BindsInstance
import dagger.Component
import im.vector.riotx.core.error.ErrorFormatter
import im.vector.riotx.core.preference.UserAvatarPreference
import im.vector.riotx.features.MainActivity
import im.vector.riotx.features.crypto.keysbackup.settings.KeysBackupManageActivity
import im.vector.riotx.features.home.HomeActivity
import im.vector.riotx.features.home.HomeModule
import im.vector.riotx.features.createdirect.CreateDirectRoomActivity
import im.vector.riotx.features.home.room.detail.readreceipts.DisplayReadReceiptsBottomSheet
import im.vector.riotx.features.home.room.detail.timeline.action.MessageActionsBottomSheet
import im.vector.riotx.features.home.room.detail.timeline.edithistory.ViewEditHistoryBottomSheet
import im.vector.riotx.features.home.room.detail.timeline.reactions.ViewReactionsBottomSheet
import im.vector.riotx.features.home.room.filtered.FilteredRoomsActivity
import im.vector.riotx.features.home.room.list.RoomListModule
import im.vector.riotx.features.home.room.list.actions.RoomListQuickActionsBottomSheet
import im.vector.riotx.features.invite.VectorInviteView
import im.vector.riotx.features.link.LinkHandlerActivity
import im.vector.riotx.features.login.LoginActivity
import im.vector.riotx.features.media.ImageMediaViewerActivity
import im.vector.riotx.features.media.VideoMediaViewerActivity
import im.vector.riotx.features.navigation.Navigator
import im.vector.riotx.features.permalink.PermalinkHandlerActivity
import im.vector.riotx.features.rageshake.BugReportActivity
import im.vector.riotx.features.rageshake.BugReporter
import im.vector.riotx.features.rageshake.RageShake
import im.vector.riotx.features.reactions.EmojiReactionPickerActivity
import im.vector.riotx.features.reactions.widget.ReactionButton
import im.vector.riotx.features.roomdirectory.RoomDirectoryActivity
import im.vector.riotx.features.roomdirectory.createroom.CreateRoomActivity
import im.vector.riotx.features.settings.VectorSettingsActivity
import im.vector.riotx.features.share.IncomingShareActivity
import im.vector.riotx.features.signout.soft.SoftLogoutActivity
import im.vector.riotx.features.ui.UiStateRepository

@Component(
        dependencies = [
            VectorComponent::class
        ],
        modules = [
            AssistedInjectModule::class,
            ViewModelModule::class,
            FragmentModule::class,
            HomeModule::class,
            RoomListModule::class,
            ScreenModule::class
        ]
)
@ScreenScope
interface ScreenComponent {

    fun activeSessionHolder(): ActiveSessionHolder

    fun fragmentFactory(): FragmentFactory

    fun viewModelFactory(): ViewModelProvider.Factory

    fun bugReporter(): BugReporter

    fun rageShake(): RageShake

    fun navigator(): Navigator

    fun errorFormatter(): ErrorFormatter

    fun uiStateRepository(): UiStateRepository

    fun inject(activity: HomeActivity)

    fun inject(messageActionsBottomSheet: MessageActionsBottomSheet)

    fun inject(viewReactionsBottomSheet: ViewReactionsBottomSheet)

    fun inject(viewEditHistoryBottomSheet: ViewEditHistoryBottomSheet)

    fun inject(vectorSettingsActivity: VectorSettingsActivity)

    fun inject(keysBackupManageActivity: KeysBackupManageActivity)

    fun inject(emojiReactionPickerActivity: EmojiReactionPickerActivity)

    fun inject(loginActivity: LoginActivity)

    fun inject(linkHandlerActivity: LinkHandlerActivity)

    fun inject(mainActivity: MainActivity)

    fun inject(roomDirectoryActivity: RoomDirectoryActivity)

    fun inject(bugReportActivity: BugReportActivity)

    fun inject(imageMediaViewerActivity: ImageMediaViewerActivity)

    fun inject(filteredRoomsActivity: FilteredRoomsActivity)

    fun inject(createRoomActivity: CreateRoomActivity)

    fun inject(vectorInviteView: VectorInviteView)

    fun inject(videoMediaViewerActivity: VideoMediaViewerActivity)

    fun inject(userAvatarPreference: UserAvatarPreference)

    fun inject(createDirectRoomActivity: CreateDirectRoomActivity)

    fun inject(displayReadReceiptsBottomSheet: DisplayReadReceiptsBottomSheet)

    fun inject(reactionButton: ReactionButton)

    fun inject(incomingShareActivity: IncomingShareActivity)

    fun inject(roomListActionsBottomSheet: RoomListQuickActionsBottomSheet)

    fun inject(activity: SoftLogoutActivity)

    fun inject(permalinkHandlerActivity: PermalinkHandlerActivity)

    @Component.Factory
    interface Factory {
        fun create(vectorComponent: VectorComponent,
                   @BindsInstance context: AppCompatActivity
        ): ScreenComponent
    }
}
