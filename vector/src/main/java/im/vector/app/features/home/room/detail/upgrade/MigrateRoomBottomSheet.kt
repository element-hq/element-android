/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.home.room.detail.upgrade

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.setFragmentResult
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.core.error.ErrorFormatter
import im.vector.app.core.extensions.setTextOrHide
import im.vector.app.core.platform.VectorBaseBottomSheetDialogFragment
import im.vector.app.databinding.BottomSheetRoomUpgradeBinding
import im.vector.lib.strings.CommonStrings
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@AndroidEntryPoint
class MigrateRoomBottomSheet :
        VectorBaseBottomSheetDialogFragment<BottomSheetRoomUpgradeBinding>() {

    enum class MigrationReason {
        MANUAL,
        FOR_RESTRICTED
    }

    @Parcelize
    data class Args(
            val roomId: String,
            val newVersion: String,
            val reason: MigrationReason = MigrationReason.MANUAL,
            val customDescription: CharSequence? = null
    ) : Parcelable

    override val showExpanded = true

    @Inject lateinit var errorFormatter: ErrorFormatter

    val viewModel: MigrateRoomViewModel by fragmentViewModel()

    override fun invalidate() = withState(viewModel) { state ->
        views.headerText.setText(if (state.isPublic) CommonStrings.upgrade_public_room else CommonStrings.upgrade_private_room)

        if (state.migrationReason == MigrationReason.MANUAL) {
            views.descriptionText.text = getString(CommonStrings.upgrade_room_warning)
            views.upgradeFromTo.text = getString(CommonStrings.upgrade_public_room_from_to, state.currentVersion, state.newVersion)
        } else if (state.migrationReason == MigrationReason.FOR_RESTRICTED) {
            views.descriptionText.setTextOrHide(state.customDescription)
            views.upgradeFromTo.text = getString(CommonStrings.upgrade_room_for_restricted_note)
        }

        if (state.autoMigrateMembersAndParents) {
            views.autoUpdateParent.isVisible = false
            views.autoInviteSwitch.isVisible = false
        } else {
            views.autoInviteSwitch.isVisible = !state.isPublic && state.otherMemberCount > 0
            views.autoUpdateParent.isVisible = state.knownParents.isNotEmpty()
        }

        when (state.upgradingStatus) {
            is Loading -> {
                views.progressBar.isVisible = true
                views.progressBar.isIndeterminate = state.upgradingProgressIndeterminate
                views.progressBar.progress = state.upgradingProgress
                views.progressBar.max = state.upgradingProgressTotal
                views.inlineError.setTextOrHide(null)
                views.button.isVisible = false
            }
            is Success -> {
                views.progressBar.isVisible = false
                when (val result = state.upgradingStatus.invoke()) {
                    is UpgradeRoomViewModelTask.Result.Failure -> {
                        val errorText = when (result) {
                            is UpgradeRoomViewModelTask.Result.UnknownRoom -> {
                                // should not happen
                                getString(CommonStrings.unknown_error)
                            }
                            is UpgradeRoomViewModelTask.Result.NotAllowed -> {
                                getString(CommonStrings.upgrade_room_no_power_to_manage)
                            }
                            is UpgradeRoomViewModelTask.Result.ErrorFailure -> {
                                errorFormatter.toHumanReadable(result.throwable)
                            }
                            else -> null
                        }
                        views.inlineError.setTextOrHide(errorText)
                        views.button.isVisible = true
                        views.button.text = getString(CommonStrings.global_retry)
                    }
                    is UpgradeRoomViewModelTask.Result.Success -> {
                        setFragmentResult(REQUEST_KEY, Bundle().apply {
                            putString(BUNDLE_KEY_REPLACEMENT_ROOM, result.replacementRoomId)
                        })
                        dismiss()
                    }
                }
            }
            else -> {
                views.button.isVisible = true
                views.button.text = getString(CommonStrings.upgrade)
            }
        }

        super.invalidate()
    }

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?) =
            BottomSheetRoomUpgradeBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        views.button.debouncedClicks {
            viewModel.handle(MigrateRoomAction.UpgradeRoom)
        }

        views.autoInviteSwitch.setOnCheckedChangeListener { _, isChecked ->
            viewModel.handle(MigrateRoomAction.SetAutoInvite(isChecked))
        }

        views.autoUpdateParent.setOnCheckedChangeListener { _, isChecked ->
            viewModel.handle(MigrateRoomAction.SetUpdateKnownParentSpace(isChecked))
        }
    }

    companion object {

        const val REQUEST_KEY = "MigrateRoomBottomSheetRequest"
        const val BUNDLE_KEY_REPLACEMENT_ROOM = "BUNDLE_KEY_REPLACEMENT_ROOM"

        fun newInstance(
                roomId: String, newVersion: String,
                reason: MigrationReason = MigrationReason.MANUAL,
                customDescription: CharSequence? = null
        ): MigrateRoomBottomSheet {
            return MigrateRoomBottomSheet().apply {
                setArguments(Args(roomId, newVersion, reason, customDescription))
            }
        }
    }
}
