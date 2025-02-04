/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.roomprofile.settings.joinrule

import android.os.Bundle
import android.os.Parcelable
import android.view.View
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.core.ui.bottomsheet.BottomSheetGeneric
import im.vector.app.core.ui.bottomsheet.BottomSheetGenericController
import kotlinx.parcelize.Parcelize
import org.matrix.android.sdk.api.session.room.model.RoomJoinRules
import javax.inject.Inject

@Parcelize
data class JoinRulesOptionSupport(
        val rule: RoomJoinRules,
        val needUpgrade: Boolean = false
) : Parcelable

fun RoomJoinRules.toOption(needUpgrade: Boolean) = JoinRulesOptionSupport(this, needUpgrade)

@Parcelize
data class RoomJoinRuleBottomSheetArgs(
        val currentRoomJoinRule: RoomJoinRules,
        val allowedJoinedRules: List<JoinRulesOptionSupport>,
        val isSpace: Boolean = false,
        val parentSpaceName: String?
) : Parcelable

@AndroidEntryPoint
class RoomJoinRuleBottomSheet : BottomSheetGeneric<RoomJoinRuleState, RoomJoinRuleRadioAction>() {

    private lateinit var roomJoinRuleSharedActionViewModel: RoomJoinRuleSharedActionViewModel
    @Inject lateinit var controller: RoomJoinRuleController
    private val viewModel: RoomJoinRuleViewModel by fragmentViewModel(RoomJoinRuleViewModel::class)

    override fun getController(): BottomSheetGenericController<RoomJoinRuleState, RoomJoinRuleRadioAction> = controller

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        roomJoinRuleSharedActionViewModel = activityViewModelProvider.get(RoomJoinRuleSharedActionViewModel::class.java)
    }

    override fun didSelectAction(action: RoomJoinRuleRadioAction) {
        roomJoinRuleSharedActionViewModel.post(action)
        dismiss()
    }

    override fun invalidate() = withState(viewModel) {
        controller.setData(it)
        super.invalidate()
    }

    companion object {
        fun newInstance(
                currentRoomJoinRule: RoomJoinRules,
                allowedJoinedRules: List<JoinRulesOptionSupport> = listOf(
                        RoomJoinRules.INVITE, RoomJoinRules.PUBLIC
                ).map { it.toOption(true) },
                isSpace: Boolean = false,
                parentSpaceName: String? = null
        ): RoomJoinRuleBottomSheet {
            return RoomJoinRuleBottomSheet().apply {
                setArguments(
                        RoomJoinRuleBottomSheetArgs(currentRoomJoinRule, allowedJoinedRules, isSpace, parentSpaceName)
                )
            }
        }
    }
}
