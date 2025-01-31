/*
 * Copyright 2020-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.crypto.verification.qrconfirmation

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.airbnb.mvrx.Mavericks
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.core.extensions.cleanup
import im.vector.app.core.extensions.configureWith
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.databinding.BottomSheetVerificationChildFragmentBinding
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@AndroidEntryPoint
class VerificationQRWaitingFragment :
        VectorBaseFragment<BottomSheetVerificationChildFragmentBinding>() {

    @Inject lateinit var controller: VerificationQRWaitingController

    @Parcelize
    data class Args(
            val isMe: Boolean,
            val otherUserName: String
    ) : Parcelable

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): BottomSheetVerificationChildFragmentBinding {
        return BottomSheetVerificationChildFragmentBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        (arguments?.getParcelable(Mavericks.KEY_ARG) as? Args)?.let {
            controller.update(it)
        }
    }

    override fun onDestroyView() {
        views.bottomSheetVerificationRecyclerView.cleanup()
        super.onDestroyView()
    }

    private fun setupRecyclerView() {
        views.bottomSheetVerificationRecyclerView.configureWith(controller, hasFixedSize = false, disableItemAnimation = true)
    }
}
