/*
 * Copyright 2019 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
import im.vector.app.R
import im.vector.app.core.extensions.cleanup
import im.vector.app.core.extensions.configureWith
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.databinding.FragmentGenericRecyclerBinding
import org.matrix.android.sdk.api.session.pushers.Pusher
import javax.inject.Inject

// Referenced in vector_settings_notifications.xml
class PushGatewaysFragment @Inject constructor(
        private val epoxyController: PushGateWayController
) : VectorBaseFragment<FragmentGenericRecyclerBinding>() {

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentGenericRecyclerBinding {
        return FragmentGenericRecyclerBinding.inflate(inflater, container, false)
    }

    private val viewModel: PushGatewaysViewModel by fragmentViewModel(PushGatewaysViewModel::class)

    override fun getMenuRes() = R.menu.menu_push_gateways

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.refresh -> {
                viewModel.handle(PushGatewayAction.Refresh)
                true
            }
            else         ->
                super.onOptionsItemSelected(item)
        }
    }

    override fun onResume() {
        super.onResume()
        (activity as? AppCompatActivity)?.supportActionBar?.setTitle(R.string.settings_notifications_targets)
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
                            .setTitle(R.string.dialog_title_error)
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
