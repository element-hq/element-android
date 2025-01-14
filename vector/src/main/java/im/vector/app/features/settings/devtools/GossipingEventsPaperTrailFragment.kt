/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.settings.devtools

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.R
import im.vector.app.core.extensions.cleanup
import im.vector.app.core.extensions.configureWith
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.databinding.FragmentGenericRecyclerBinding
import org.matrix.android.sdk.api.session.crypto.model.AuditTrail
import javax.inject.Inject

@AndroidEntryPoint
class GossipingEventsPaperTrailFragment :
        VectorBaseFragment<FragmentGenericRecyclerBinding>(),
        GossipingTrailPagedEpoxyController.InteractionListener {

    @Inject lateinit var epoxyController: GossipingTrailPagedEpoxyController

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentGenericRecyclerBinding {
        return FragmentGenericRecyclerBinding.inflate(inflater, container, false)
    }

    private val viewModel: GossipingEventsPaperTrailViewModel by fragmentViewModel(GossipingEventsPaperTrailViewModel::class)

    override fun invalidate() = withState(viewModel) { state ->
        state.events.invoke()?.let {
            epoxyController.submitList(it)
        }
        Unit
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        views.genericRecyclerView.configureWith(epoxyController, dividerDrawable = R.drawable.divider_horizontal)
        epoxyController.interactionListener = this
    }

    override fun onDestroyView() {
        views.genericRecyclerView.cleanup()
        epoxyController.interactionListener = null
        super.onDestroyView()
    }

    override fun didTap(event: AuditTrail) {
//        if (event.isEncrypted()) {
//            event.toClearContentStringWithIndent()
//        } else {
//            event.toContentStringWithIndent()
//        }?.let {
//            JSonViewerDialog.newInstance(
//                    it,
//                    -1,
//                    createJSonViewerStyleProvider(colorProvider)
//            ).show(childFragmentManager, "JSON_VIEWER")
//        }
    }
}
