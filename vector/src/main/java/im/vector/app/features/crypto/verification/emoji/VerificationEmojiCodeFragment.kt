/*
 * Copyright 2019-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package im.vector.app.features.crypto.verification.emoji

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.parentFragmentViewModel
import com.airbnb.mvrx.withState
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.core.extensions.cleanup
import im.vector.app.core.extensions.configureWith
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.databinding.BottomSheetVerificationChildFragmentBinding
import im.vector.app.features.crypto.verification.VerificationAction
import im.vector.app.features.crypto.verification.VerificationBottomSheetViewModel
import javax.inject.Inject

@AndroidEntryPoint
class VerificationEmojiCodeFragment :
        VectorBaseFragment<BottomSheetVerificationChildFragmentBinding>(),
        VerificationEmojiCodeController.Listener {

    @Inject lateinit var controller: VerificationEmojiCodeController

    private val viewModel by fragmentViewModel(VerificationEmojiCodeViewModel::class)

    private val sharedViewModel by parentFragmentViewModel(VerificationBottomSheetViewModel::class)

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): BottomSheetVerificationChildFragmentBinding {
        return BottomSheetVerificationChildFragmentBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
    }

    override fun onDestroyView() {
        views.bottomSheetVerificationRecyclerView.cleanup()
        controller.listener = null
        super.onDestroyView()
    }

    private fun setupRecyclerView() {
        views.bottomSheetVerificationRecyclerView.configureWith(controller, hasFixedSize = false, disableItemAnimation = true)
        controller.listener = this
    }

    override fun invalidate() = withState(viewModel) { state ->
        controller.update(state)
    }

    override fun onMatchButtonTapped() = withState(viewModel) { state ->
        val otherUserId = state.otherUser.id
        val txId = state.transactionId ?: return@withState
        sharedViewModel.handle(VerificationAction.SASMatchAction(otherUserId, txId))
    }

    override fun onDoNotMatchButtonTapped() = withState(viewModel) { state ->
        val otherUserId = state.otherUser.id
        val txId = state.transactionId ?: return@withState
        sharedViewModel.handle(VerificationAction.SASDoNotMatchAction(otherUserId, txId))
    }
}
