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

package im.vector.app.features.home.room.detail.composer.link

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isGone
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.airbnb.mvrx.args
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.core.platform.VectorBaseDialogFragment
import im.vector.app.databinding.FragmentSetLinkBinding
import im.vector.lib.strings.CommonStrings
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.parcelize.Parcelize
import reactivecircus.flowbinding.android.widget.textChanges

@AndroidEntryPoint
class SetLinkFragment :
        VectorBaseDialogFragment<FragmentSetLinkBinding>() {

    @Parcelize
    data class Args(
            val isTextSupported: Boolean,
            val initialLink: String?,
    ) : Parcelable

    private val viewModel: SetLinkViewModel by fragmentViewModel()
    private val sharedActionViewModel: SetLinkSharedActionViewModel by viewModels(
            ownerProducer = { requireParentFragment() }
    )
    private val args: Args by args()

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentSetLinkBinding {
        return FragmentSetLinkBinding.inflate(inflater, container, false)
    }

    companion object {
        fun show(isTextSupported: Boolean, initialLink: String?, fragmentManager: FragmentManager) =
                SetLinkFragment().apply {
                    setArguments(Args(isTextSupported, initialLink))
                }.show(fragmentManager, "SetLinkBottomSheet")
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        views.link.setText(args.initialLink)
        views.link.textChanges()
                .onEach {
                    viewModel.handle(SetLinkAction.LinkChanged(it.toString()))
                }
                .launchIn(viewLifecycleOwner.lifecycleScope)

        views.save.debouncedClicks {
            viewModel.handle(
                    SetLinkAction.Save(
                            link = views.link.text.toString(),
                            text = views.text.text.toString(),
                    )
            )
        }

        views.cancel.debouncedClicks(::onCancel)
        views.remove.debouncedClicks(::onRemove)

        viewModel.observeViewEvents {
            when (it) {
                is SetLinkViewEvents.SavedLinkAndText -> handleInsert(link = it.link, text = it.text)
                is SetLinkViewEvents.SavedLink -> handleSet(link = it.link)
            }
        }

        views.toolbar.setNavigationOnClickListener {
            dismiss()
        }
    }

    override fun invalidate() = withState(viewModel) { viewState ->
        views.toolbar.title = getString(
                if (viewState.initialLink != null) {
                    CommonStrings.set_link_edit
                } else {
                    CommonStrings.set_link_create
                }
        )

        views.remove.isGone = !viewState.removeVisible
        views.save.isEnabled = viewState.saveEnabled
        views.textLayout.isGone = !viewState.isTextSupported
    }

    private fun handleInsert(link: String, text: String) {
        sharedActionViewModel.post(SetLinkSharedAction.Insert(text, link))
        dismiss()
    }

    private fun handleSet(link: String) {
        sharedActionViewModel.post(SetLinkSharedAction.Set(link))
        dismiss()
    }

    private fun onRemove() {
        sharedActionViewModel.post(SetLinkSharedAction.Remove)
        dismiss()
    }

    private fun onCancel() = dismiss()
}
