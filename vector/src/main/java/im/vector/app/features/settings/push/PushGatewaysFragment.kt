/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.settings.push

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.R
import im.vector.app.core.extensions.cleanup
import im.vector.app.core.extensions.configureWith
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.core.platform.VectorMenuProvider
import im.vector.app.databinding.FragmentGenericRecyclerBinding
import im.vector.lib.strings.CommonStrings
import org.matrix.android.sdk.api.session.pushers.Pusher
import javax.inject.Inject

// Referenced in vector_settings_notifications.xml
@AndroidEntryPoint
class PushGatewaysFragment :
        VectorBaseFragment<FragmentGenericRecyclerBinding>(),
        VectorMenuProvider {

    @Inject lateinit var epoxyController: PushGateWayController

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentGenericRecyclerBinding {
        return FragmentGenericRecyclerBinding.inflate(inflater, container, false)
    }

    private val viewModel: PushGatewaysViewModel by fragmentViewModel(PushGatewaysViewModel::class)

    override fun getMenuRes() = R.menu.menu_push_gateways

    override fun handleMenuItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.refresh -> {
                viewModel.handle(PushGatewayAction.Refresh)
                true
            }
            else -> false
        }
    }

    override fun onResume() {
        super.onResume()
        (activity as? AppCompatActivity)?.supportActionBar?.setTitle(CommonStrings.settings_notifications_targets)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        epoxyController.interactionListener = object : PushGatewayItemInteractions {
            override fun onRemovePushTapped(pusher: Pusher) = viewModel.handle(PushGatewayAction.RemovePusher(pusher))
        }
        views.genericRecyclerView.configureWith(epoxyController, dividerDrawable = R.drawable.divider_horizontal)
        viewModel.observeViewEvents {
            when (it) {
                is PushGatewayViewEvents.RemovePusherFailed -> {
                    MaterialAlertDialogBuilder(requireContext())
                            .setTitle(CommonStrings.dialog_title_error)
                            .setMessage(errorFormatter.toHumanReadable(it.cause))
                            .setPositiveButton(android.R.string.ok, null)
                            .show()
                }
            }
        }
    }

    override fun onDestroyView() {
        views.genericRecyclerView.cleanup()
        super.onDestroyView()
    }

    override fun invalidate() = withState(viewModel) { state ->
        epoxyController.setData(state)
    }
}
