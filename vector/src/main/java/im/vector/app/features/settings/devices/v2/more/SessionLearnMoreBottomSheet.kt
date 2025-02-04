/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.settings.devices.v2.more

import android.content.DialogInterface
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.core.platform.VectorBaseBottomSheetDialogFragment
import im.vector.app.databinding.BottomSheetSessionLearnMoreBinding
import kotlinx.parcelize.Parcelize

@AndroidEntryPoint
class SessionLearnMoreBottomSheet : VectorBaseBottomSheetDialogFragment<BottomSheetSessionLearnMoreBinding>() {

    @Parcelize
    data class Args(
            val title: String,
            val description: String,
    ) : Parcelable

    private val viewModel: SessionLearnMoreViewModel by fragmentViewModel()

    override val showExpanded = true

    var onDismiss: (() -> Unit)? = null

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): BottomSheetSessionLearnMoreBinding {
        return BottomSheetSessionLearnMoreBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initCloseButton()
    }

    private fun initCloseButton() {
        views.bottomSheetSessionLearnMoreCloseButton.debouncedClicks {
            dismiss()
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        onDismiss?.invoke()
    }

    override fun invalidate() = withState(viewModel) { viewState ->
        super.invalidate()
        views.bottomSheetSessionLearnMoreTitle.text = viewState.title
        views.bottomSheetSessionLearnMoreDescription.text = viewState.description
    }

    companion object {

        fun show(fragmentManager: FragmentManager, args: Args): SessionLearnMoreBottomSheet {
            val bottomSheet = SessionLearnMoreBottomSheet()
            bottomSheet.isCancelable = true
            bottomSheet.setArguments(args)
            bottomSheet.show(fragmentManager, "SessionLearnMoreBottomSheet")
            return bottomSheet
        }
    }
}
