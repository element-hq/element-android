/*
 * Copyright (c) 2021 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.app.features.roomprofile.settings.joinrule

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.airbnb.mvrx.activityViewModel
import com.airbnb.mvrx.withState
import im.vector.app.R
import im.vector.app.core.extensions.commitTransaction
import im.vector.app.core.extensions.configureWith
import im.vector.app.core.platform.OnBackPressed
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.databinding.FragmentJoinRulesRecyclerBinding
import im.vector.app.features.home.AvatarRenderer
import im.vector.app.features.roomprofile.settings.joinrule.advanced.RoomJoinRuleChooseRestrictedViewModel
import org.matrix.android.sdk.api.session.room.model.RoomJoinRules
import javax.inject.Inject

class RoomJoinRuleFragment @Inject constructor(
        val controller: RoomJoinRuleAdvancedController,
//        val viewModelFactory: RoomJoinRuleAdvancedViewModel.Factory,
        val avatarRenderer: AvatarRenderer
) : VectorBaseFragment<FragmentJoinRulesRecyclerBinding>(),
//        RoomJoinRuleAdvancedViewModel.Factory,
        OnBackPressed, RoomJoinRuleAdvancedController.InteractionListener {

    private val viewModel: RoomJoinRuleChooseRestrictedViewModel by activityViewModel()

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?) =
            FragmentJoinRulesRecyclerBinding.inflate(inflater, container, false)

    override fun onBackPressed(toolbarButton: Boolean): Boolean {
        // TODO
        requireActivity().finish()
        return true
    }

    override fun invalidate() = withState(viewModel) { state ->
        super.invalidate()
        controller.setData(state)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
//        roomProfileSharedActionViewModel = activityViewModelProvider.get(RoomProfileSharedActionViewModel::class.java)
//        setupRoomHistoryVisibilitySharedActionViewModel()
//        setupRoomJoinRuleSharedActionViewModel()
//        controller.callback = this
        views.genericRecyclerView.configureWith(controller, hasFixedSize = true)
        controller.interactionListener = this
//        views.waitingView.waitingStatusText.setText(R.string.please_wait)
//        views.waitingView.waitingStatusText.isVisible = true

//        // Use the Kotlin extension in the fragment-ktx artifact
//        setFragmentResultListener("SelectAllowList") { requestKey, bundle ->
//            // We use a String here, but any type that can be put in a Bundle is supported
//            bundle.getStringArrayList("bundleKey")?.toList()?.let {
//                 viewModel.handle(RoomJoinRuleAdvancedAction.UpdateAllowList(it))
//             }
//        }

        viewModel.observeViewEvents {
            when (it) {
                RoomJoinRuleAdvancedEvents.SelectAllowList -> {
                    parentFragmentManager.commitTransaction {
                        setCustomAnimations(R.anim.fade_in, R.anim.fade_out, R.anim.fade_in, R.anim.fade_out)
                        val tag = RoomJoinRuleChooseRestrictedFragment::class.simpleName
                        replace(R.id.simpleFragmentContainer,
                                RoomJoinRuleChooseRestrictedFragment::class.java,
                                this@RoomJoinRuleFragment.arguments,
                                tag
                        ).addToBackStack(tag)
                    }
                }
            }
        }
    }

    override fun create(initialState: RoomJoinRuleAdvancedState) = viewModelFactory.create(initialState)

    override fun didSelectRule(rules: RoomJoinRules) {
        viewModel.handle(RoomJoinRuleAdvancedAction.SelectJoinRules(rules))
    }
}
