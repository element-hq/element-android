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

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentFactory
import androidx.lifecycle.ViewModelProvider
import dagger.BindsInstance
import dagger.Component
import im.vector.app.core.dialogs.UnrecognizedCertificateDialog
import im.vector.app.core.error.ErrorFormatter
import im.vector.app.core.preference.UserAvatarPreference
import im.vector.app.features.MainActivity
import im.vector.app.features.auth.ReAuthActivity
import im.vector.app.features.call.CallControlsBottomSheet
import im.vector.app.features.call.VectorCallActivity
import im.vector.app.features.call.conference.VectorJitsiActivity
import im.vector.app.features.call.transfer.CallTransferActivity
import im.vector.app.features.createdirect.CreateDirectRoomActivity
import im.vector.app.features.crypto.keysbackup.settings.KeysBackupManageActivity
import im.vector.app.features.crypto.quads.SharedSecureStorageActivity
import im.vector.app.features.crypto.recover.BootstrapBottomSheet
import im.vector.app.features.crypto.verification.VerificationBottomSheet
import im.vector.app.features.debug.DebugMenuActivity
import im.vector.app.features.home.HomeActivity
import im.vector.app.features.home.HomeModule
import im.vector.app.features.home.room.detail.RoomDetailActivity
import im.vector.app.features.home.room.detail.readreceipts.DisplayReadReceiptsBottomSheet
import im.vector.app.features.home.room.detail.search.SearchActivity
import im.vector.app.features.home.room.detail.timeline.action.MessageActionsBottomSheet
import im.vector.app.features.home.room.detail.timeline.edithistory.ViewEditHistoryBottomSheet
import im.vector.app.features.home.room.detail.timeline.reactions.ViewReactionsBottomSheet
import im.vector.app.features.home.room.detail.widget.RoomWidgetsBottomSheet
import im.vector.app.features.home.room.filtered.FilteredRoomsActivity
import im.vector.app.features.home.room.list.RoomListModule
import im.vector.app.features.home.room.list.actions.RoomListQuickActionsBottomSheet
import im.vector.app.features.invite.InviteUsersToRoomActivity
import im.vector.app.features.invite.VectorInviteView
import im.vector.app.features.link.LinkHandlerActivity
import im.vector.app.features.login.LoginActivity
import im.vector.app.features.matrixto.MatrixToBottomSheet
import im.vector.app.features.media.BigImageViewerActivity
import im.vector.app.features.media.VectorAttachmentViewerActivity
import im.vector.app.features.navigation.Navigator
import im.vector.app.features.permalink.PermalinkHandlerActivity
import im.vector.app.features.pin.PinLocker
import im.vector.app.features.qrcode.QrCodeScannerActivity
import im.vector.app.features.rageshake.BugReportActivity
import im.vector.app.features.rageshake.BugReporter
import im.vector.app.features.rageshake.RageShake
import im.vector.app.features.reactions.EmojiReactionPickerActivity
import im.vector.app.features.reactions.widget.ReactionButton
import im.vector.app.features.roomdirectory.RoomDirectoryActivity
import im.vector.app.features.roomdirectory.createroom.CreateRoomActivity
import im.vector.app.features.roommemberprofile.RoomMemberProfileActivity
import im.vector.app.features.roommemberprofile.devices.DeviceListBottomSheet
import im.vector.app.features.roomprofile.RoomProfileActivity
import im.vector.app.features.roomprofile.alias.detail.RoomAliasBottomSheet
import im.vector.app.features.roomprofile.settings.historyvisibility.RoomHistoryVisibilityBottomSheet
import im.vector.app.features.roomprofile.settings.joinrule.RoomJoinRuleBottomSheet
import im.vector.app.features.settings.VectorSettingsActivity
import im.vector.app.features.settings.devices.DeviceVerificationInfoBottomSheet
import im.vector.app.features.share.IncomingShareActivity
import im.vector.app.features.signout.soft.SoftLogoutActivity
import im.vector.app.features.terms.ReviewTermsActivity
import im.vector.app.features.ui.UiStateRepository
import im.vector.app.features.usercode.UserCodeActivity
import im.vector.app.features.widgets.WidgetActivity
import im.vector.app.features.widgets.permissions.RoomWidgetPermissionBottomSheet
import im.vector.app.features.workers.signout.SignOutBottomSheetDialogFragment

@Component(
        dependencies = [
            AggregatorEntryPoint::class
        ],
        modules = [
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
     * BottomSheets
     * ========================================================================================== */

    fun inject(bottomSheet: RoomListQuickActionsBottomSheet)
    fun inject(bottomSheet: RoomAliasBottomSheet)
    fun inject(bottomSheet: RoomHistoryVisibilityBottomSheet)
    fun inject(bottomSheet: RoomJoinRuleBottomSheet)

    /* ==========================================================================================
     * Factory
     * ========================================================================================== */

    @Component.Factory
    interface Factory {
        fun create(vectorComponent: AggregatorEntryPoint,
                   @BindsInstance context: AppCompatActivity
        ): ScreenComponent
    }
}
