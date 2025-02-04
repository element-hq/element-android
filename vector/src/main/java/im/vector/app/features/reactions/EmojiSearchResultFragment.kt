/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package im.vector.app.features.reactions

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.airbnb.mvrx.activityViewModel
import com.airbnb.mvrx.withState
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.R
import im.vector.app.core.extensions.cleanup
import im.vector.app.core.extensions.configureWith
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.core.utils.LiveEvent
import im.vector.app.databinding.FragmentGenericRecyclerBinding
import javax.inject.Inject

@AndroidEntryPoint
class EmojiSearchResultFragment :
        VectorBaseFragment<FragmentGenericRecyclerBinding>(),
        ReactionClickListener {

    @Inject lateinit var epoxyController: EmojiSearchResultController

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentGenericRecyclerBinding {
        return FragmentGenericRecyclerBinding.inflate(inflater, container, false)
    }

    private val viewModel: EmojiSearchResultViewModel by activityViewModel()

    private lateinit var sharedViewModel: EmojiChooserViewModel

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sharedViewModel = activityViewModelProvider.get(EmojiChooserViewModel::class.java)
        epoxyController.listener = this
        views.genericRecyclerView.configureWith(epoxyController, dividerDrawable = R.drawable.divider_horizontal)
    }

    override fun onDestroyView() {
        epoxyController.listener = null
        views.genericRecyclerView.cleanup()
        super.onDestroyView()
    }

    override fun onReactionSelected(reaction: String) {
        sharedViewModel.selectedReaction = reaction
        sharedViewModel.navigateEvent.value = LiveEvent(EmojiChooserViewModel.NAVIGATE_FINISH)
    }

    override fun invalidate() = withState(viewModel) { state ->
        epoxyController.setData(state)
    }
}
