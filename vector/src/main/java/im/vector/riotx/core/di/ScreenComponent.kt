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
import im.vector.riotx.core.dialogs.UnrecognizedCertificateDialog
import im.vector.riotx.core.error.ErrorFormatter
import im.vector.riotx.core.preference.UserAvatarPreference
import im.vector.riotx.features.MainActivity
import im.vector.riotx.features.call.CallControlsBottomSheet
import im.vector.riotx.features.call.VectorCallActivity
import im.vector.riotx.features.createdirect.CreateDirectRoomActivity
import im.vector.riotx.features.crypto.keysbackup.settings.KeysBackupManageActivity
import im.vector.riotx.features.crypto.quads.SharedSecureStorageActivity
import im.vector.riotx.features.crypto.recover.BootstrapBottomSheet
import im.vector.riotx.features.crypto.verification.VerificationBottomSheet
import im.vector.riotx.features.debug.DebugMenuActivity
import im.vector.riotx.features.home.HomeActivity
import im.vector.riotx.features.home.HomeModule
import im.vector.riotx.features.home.room.detail.RoomDetailActivity
import im.vector.riotx.features.home.room.detail.readreceipts.DisplayReadReceiptsBottomSheet
import im.vector.riotx.features.home.room.detail.timeline.action.MessageActionsBottomSheet
import im.vector.riotx.features.home.room.detail.timeline.edithistory.ViewEditHistoryBottomSheet
import im.vector.riotx.features.home.room.detail.timeline.reactions.ViewReactionsBottomSheet
import im.vector.riotx.features.home.room.detail.widget.RoomWidgetsBottomSheet
import im.vector.riotx.features.home.room.filtered.FilteredRoomsActivity
import im.vector.riotx.features.home.room.list.RoomListModule
import im.vector.riotx.features.home.room.list.actions.RoomListQuickActionsBottomSheet
import im.vector.riotx.features.invite.InviteUsersToRoomActivity
import im.vector.riotx.features.invite.VectorInviteView
import im.vector.riotx.features.link.LinkHandlerActivity
import im.vector.riotx.features.login.LoginActivity
import im.vector.riotx.features.media.VectorAttachmentViewerActivity
import im.vector.riotx.features.media.BigImageViewerActivity
import im.vector.riotx.features.media.ImageMediaViewerActivity
import im.vector.riotx.features.media.VideoMediaViewerActivity
import im.vector.riotx.features.navigation.Navigator
import im.vector.riotx.features.permalink.PermalinkHandlerActivity
import im.vector.riotx.features.pin.PinLocker
import im.vector.riotx.features.qrcode.QrCodeScannerActivity
import im.vector.riotx.features.rageshake.BugReportActivity
import im.vector.riotx.features.rageshake.BugReporter
import im.vector.riotx.features.rageshake.RageShake
import im.vector.riotx.features.reactions.EmojiReactionPickerActivity
import im.vector.riotx.features.reactions.widget.ReactionButton
import im.vector.riotx.features.roomdirectory.RoomDirectoryActivity
import im.vector.riotx.features.roomdirectory.createroom.CreateRoomActivity
import im.vector.riotx.features.roommemberprofile.RoomMemberProfileActivity
import im.vector.riotx.features.roommemberprofile.devices.DeviceListBottomSheet
import im.vector.riotx.features.roomprofile.RoomProfileActivity
import im.vector.riotx.features.settings.VectorSettingsActivity
import im.vector.riotx.features.settings.devices.DeviceVerificationInfoBottomSheet
import im.vector.riotx.features.share.IncomingShareActivity
import im.vector.riotx.features.signout.soft.SoftLogoutActivity
import im.vector.riotx.features.terms.ReviewTermsActivity
import im.vector.riotx.features.ui.UiStateRepository
import im.vector.riotx.features.widgets.WidgetActivity
import im.vector.riotx.features.widgets.permissions.RoomWidgetPermissionBottomSheet
import im.vector.riotx.features.workers.signout.SignOutBottomSheetDialogFragment

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

    /* ==========================================================================================
     * Shortcut to VectorComponent elements
     * ========================================================================================== */

    fun activeSessionHolder(): ActiveSessionHolder
    fun fragmentFactory(): FragmentFactory
    fun viewModelFactory(): ViewModelProvider.Factory
    fun bugReporter(): BugReporter
    fun rageShake(): RageShake
    fun navigator(): Navigator
    fun pinLocker(): PinLocker
    fun errorFormatter(): ErrorFormatter
    fun uiStateRepository(): UiStateRepository
    fun unrecognizedCertificateDialog(): UnrecognizedCertificateDialog

    /* ==========================================================================================
     * Activities
     * ========================================================================================== */

    fun inject(activity: HomeActivity)
    fun inject(activity: RoomDetailActivity)
    fun inject(activity: RoomProfileActivity)
    fun inject(activity: RoomMemberProfileActivity)
    fun inject(activity: VectorSettingsActivity)
    fun inject(activity: KeysBackupManageActivity)
    fun inject(activity: EmojiReactionPickerActivity)
    fun inject(activity: LoginActivity)
    fun inject(activity: LinkHandlerActivity)
    fun inject(activity: MainActivity)
    fun inject(activity: RoomDirectoryActivity)
    fun inject(activity: BugReportActivity)
    fun inject(activity: ImageMediaViewerActivity)
    fun inject(activity: FilteredRoomsActivity)
    fun inject(activity: CreateRoomActivity)
    fun inject(activity: VideoMediaViewerActivity)
    fun inject(activity: CreateDirectRoomActivity)
    fun inject(activity: IncomingShareActivity)
    fun inject(activity: SoftLogoutActivity)
    fun inject(activity: PermalinkHandlerActivity)
    fun inject(activity: QrCodeScannerActivity)
    fun inject(activity: DebugMenuActivity)
    fun inject(activity: SharedSecureStorageActivity)
    fun inject(activity: BigImageViewerActivity)
    fun inject(activity: InviteUsersToRoomActivity)
    fun inject(activity: ReviewTermsActivity)
    fun inject(activity: WidgetActivity)
    fun inject(activity: VectorCallActivity)
    fun inject(activity: VectorAttachmentViewerActivity)

    /* ==========================================================================================
     * BottomSheets
     * ========================================================================================== */

    fun inject(bottomSheet: MessageActionsBottomSheet)
    fun inject(bottomSheet: ViewReactionsBottomSheet)
    fun inject(bottomSheet: ViewEditHistoryBottomSheet)
    fun inject(bottomSheet: DisplayReadReceiptsBottomSheet)
    fun inject(bottomSheet: RoomListQuickActionsBottomSheet)
    fun inject(bottomSheet: VerificationBottomSheet)
    fun inject(bottomSheet: DeviceVerificationInfoBottomSheet)
    fun inject(bottomSheet: DeviceListBottomSheet)
    fun inject(bottomSheet: BootstrapBottomSheet)
    fun inject(bottomSheet: RoomWidgetPermissionBottomSheet)
    fun inject(bottomSheet: RoomWidgetsBottomSheet)
    fun inject(bottomSheet: CallControlsBottomSheet)
    fun inject(bottomSheet: SignOutBottomSheetDialogFragment)

    /* ==========================================================================================
     * Others
     * ========================================================================================== */

    fun inject(view: VectorInviteView)
    fun inject(preference: UserAvatarPreference)
    fun inject(button: ReactionButton)

    /* ==========================================================================================
     * Factory
     * ========================================================================================== */

    @Component.Factory
    interface Factory {
        fun create(vectorComponent: VectorComponent,
                   @BindsInstance context: AppCompatActivity
        ): ScreenComponent
    }
}
