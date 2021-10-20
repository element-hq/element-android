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

import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentFactory
import androidx.lifecycle.ViewModelProvider
import dagger.BindsInstance
import dagger.Component
import im.vector.app.core.dialogs.UnrecognizedCertificateDialog
import im.vector.app.core.error.ErrorFormatter
import im.vector.app.core.preference.UserAvatarPreference
import im.vector.app.features.call.CallControlsBottomSheet
import im.vector.app.features.crypto.recover.BootstrapBottomSheet
import im.vector.app.features.crypto.verification.VerificationBottomSheet
import im.vector.app.features.home.HomeModule
import im.vector.app.features.home.room.detail.JoinReplacementRoomBottomSheet
import im.vector.app.features.home.room.detail.readreceipts.DisplayReadReceiptsBottomSheet
import im.vector.app.features.home.room.detail.timeline.action.MessageActionsBottomSheet
import im.vector.app.features.home.room.detail.timeline.edithistory.ViewEditHistoryBottomSheet
import im.vector.app.features.home.room.detail.timeline.reactions.ViewReactionsBottomSheet
import im.vector.app.features.home.room.detail.upgrade.MigrateRoomBottomSheet
import im.vector.app.features.home.room.detail.widget.RoomWidgetsBottomSheet
import im.vector.app.features.home.room.list.actions.RoomListQuickActionsBottomSheet
import im.vector.app.features.invite.AutoAcceptInvites
import im.vector.app.features.invite.VectorInviteView
import im.vector.app.features.matrixto.MatrixToBottomSheet
import im.vector.app.features.navigation.Navigator
import im.vector.app.features.pin.PinLocker
import im.vector.app.features.rageshake.BugReporter
import im.vector.app.features.rageshake.RageShake
import im.vector.app.features.reactions.widget.ReactionButton
import im.vector.app.features.roommemberprofile.devices.DeviceListBottomSheet
import im.vector.app.features.roomprofile.alias.detail.RoomAliasBottomSheet
import im.vector.app.features.roomprofile.settings.historyvisibility.RoomHistoryVisibilityBottomSheet
import im.vector.app.features.roomprofile.settings.joinrule.RoomJoinRuleBottomSheet
import im.vector.app.features.settings.devices.DeviceVerificationInfoBottomSheet
import im.vector.app.features.spaces.InviteRoomSpaceChooserBottomSheet
import im.vector.app.features.spaces.LeaveSpaceBottomSheet
import im.vector.app.features.spaces.SpaceSettingsMenuBottomSheet
import im.vector.app.features.spaces.invite.SpaceInviteBottomSheet
import im.vector.app.features.spaces.share.ShareSpaceBottomSheet
import im.vector.app.features.ui.UiStateRepository
import im.vector.app.features.widgets.permissions.RoomWidgetPermissionBottomSheet
import im.vector.app.features.workers.signout.SignOutBottomSheetDialogFragment
import kotlinx.coroutines.CoroutineScope

@Component(
        dependencies = [
            SingletonEntryPoint::class
        ],
        modules = [
            ViewModelModule::class,
            FragmentModule::class,
            HomeModule::class,
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
    fun autoAcceptInvites(): AutoAcceptInvites
    fun appCoroutineScope(): CoroutineScope

    /* ==========================================================================================
     * BottomSheets
     * ========================================================================================== */

    fun inject(bottomSheet: MessageActionsBottomSheet)
    fun inject(bottomSheet: ViewReactionsBottomSheet)
    fun inject(bottomSheet: ViewEditHistoryBottomSheet)
    fun inject(bottomSheet: DisplayReadReceiptsBottomSheet)
    fun inject(bottomSheet: RoomListQuickActionsBottomSheet)
    fun inject(bottomSheet: RoomAliasBottomSheet)
    fun inject(bottomSheet: RoomHistoryVisibilityBottomSheet)
    fun inject(bottomSheet: RoomJoinRuleBottomSheet)
    fun inject(bottomSheet: VerificationBottomSheet)
    fun inject(bottomSheet: DeviceVerificationInfoBottomSheet)
    fun inject(bottomSheet: DeviceListBottomSheet)
    fun inject(bottomSheet: BootstrapBottomSheet)
    fun inject(bottomSheet: RoomWidgetPermissionBottomSheet)
    fun inject(bottomSheet: RoomWidgetsBottomSheet)
    fun inject(bottomSheet: CallControlsBottomSheet)
    fun inject(bottomSheet: SignOutBottomSheetDialogFragment)
    fun inject(bottomSheet: MatrixToBottomSheet)
    fun inject(bottomSheet: ShareSpaceBottomSheet)
    fun inject(bottomSheet: SpaceSettingsMenuBottomSheet)
    fun inject(bottomSheet: InviteRoomSpaceChooserBottomSheet)
    fun inject(bottomSheet: SpaceInviteBottomSheet)
    fun inject(bottomSheet: JoinReplacementRoomBottomSheet)
    fun inject(bottomSheet: MigrateRoomBottomSheet)
    fun inject(bottomSheet: LeaveSpaceBottomSheet)

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
        fun create(deps: SingletonEntryPoint,
                   @BindsInstance context: FragmentActivity
        ): ScreenComponent
    }
}
