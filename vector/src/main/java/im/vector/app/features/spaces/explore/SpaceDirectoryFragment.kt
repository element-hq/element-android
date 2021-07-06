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

package im.vector.app.features.spaces.explore

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.text.toSpannable
import androidx.core.view.isVisible
import com.airbnb.mvrx.activityViewModel
import com.airbnb.mvrx.withState
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import im.vector.app.R
import im.vector.app.core.extensions.cleanup
import im.vector.app.core.extensions.configureWith
import im.vector.app.core.extensions.registerStartForActivityResult
import im.vector.app.core.platform.OnBackPressed
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.core.resources.ColorProvider
import im.vector.app.core.utils.colorizeMatchingText
import im.vector.app.core.utils.isValidUrl
import im.vector.app.core.utils.openUrlInExternalBrowser
import im.vector.app.databinding.FragmentSpaceDirectoryBinding
import im.vector.app.features.home.room.detail.timeline.TimelineEventController
import im.vector.app.features.matrixto.SpaceCardRenderer
import im.vector.app.features.permalink.PermalinkHandler
import im.vector.app.features.spaces.manage.ManageType
import im.vector.app.features.spaces.manage.SpaceManageActivity
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.parcelize.Parcelize
import org.matrix.android.sdk.api.session.room.model.SpaceChildInfo
import java.net.URL
import javax.inject.Inject

@Parcelize
data class SpaceDirectoryArgs(
        val spaceId: String
) : Parcelable

class SpaceDirectoryFragment @Inject constructor(
        private val epoxyController: SpaceDirectoryController,
        private val permalinkHandler: PermalinkHandler,
        private val spaceCardRenderer: SpaceCardRenderer,
        private val colorProvider: ColorProvider
) : VectorBaseFragment<FragmentSpaceDirectoryBinding>(),
        SpaceDirectoryController.InteractionListener,
        TimelineEventController.UrlClickCallback,
        OnBackPressed {

    override fun getMenuRes() = R.menu.menu_space_directory

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?) =
            FragmentSpaceDirectoryBinding.inflate(layoutInflater, container, false)

    private val viewModel by activityViewModel(SpaceDirectoryViewModel::class)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        vectorBaseActivity.setSupportActionBar(views.toolbar)

        vectorBaseActivity.supportActionBar?.let {
            it.setDisplayShowHomeEnabled(true)
            it.setDisplayHomeAsUpEnabled(true)
        }
        epoxyController.listener = this
        views.spaceDirectoryList.configureWith(epoxyController)

        viewModel.selectSubscribe(this, SpaceDirectoryState::canAddRooms) {
            invalidateOptionsMenu()
        }

        views.spaceCard.matrixToCardMainButton.isVisible = false
        views.spaceCard.matrixToCardSecondaryButton.isVisible = false
    }

    override fun onDestroyView() {
        epoxyController.listener = null
        views.spaceDirectoryList.cleanup()
        super.onDestroyView()
    }

    override fun invalidate() = withState(viewModel) { state ->
        epoxyController.setData(state)

        val currentParent = state.hierarchyStack.lastOrNull()?.let { currentParent ->
            state.spaceSummaryApiResult.invoke()?.firstOrNull { it.childRoomId == currentParent }
        }

        if (currentParent == null) {
            val title = getString(R.string.space_explore_activity_title)
            views.toolbar.title = title

            spaceCardRenderer.render(state.spaceSummary.invoke(), emptyList(), this, views.spaceCard)
        } else {
            val title = currentParent.name ?: currentParent.canonicalAlias ?: getString(R.string.space_explore_activity_title)
            views.toolbar.title = title

            spaceCardRenderer.render(currentParent, emptyList(), this, views.spaceCard)
        }
    }

    override fun onPrepareOptionsMenu(menu: Menu) = withState(viewModel) { state ->
        menu.findItem(R.id.spaceAddRoom)?.isVisible = state.canAddRooms
        menu.findItem(R.id.spaceCreateRoom)?.isVisible = false // Not yet implemented
        super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.spaceAddRoom    -> {
                withState(viewModel) { state ->
                    addExistingRooms(state.spaceId)
                }
                return true
            }
            R.id.spaceCreateRoom -> {
                // not implemented yet
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onButtonClick(spaceChildInfo: SpaceChildInfo) {
        viewModel.handle(SpaceDirectoryViewAction.JoinOrOpen(spaceChildInfo))
    }

    override fun onSpaceChildClick(spaceChildInfo: SpaceChildInfo) {
        viewModel.handle(SpaceDirectoryViewAction.ExploreSubSpace(spaceChildInfo))
    }

    override fun onRoomClick(spaceChildInfo: SpaceChildInfo) {
        viewModel.handle(SpaceDirectoryViewAction.ShowDetails(spaceChildInfo))
    }

    override fun onBackPressed(toolbarButton: Boolean): Boolean {
        viewModel.handle(SpaceDirectoryViewAction.HandleBack)
        return true
    }

    override fun retry() {
        viewModel.handle(SpaceDirectoryViewAction.Retry)
    }

    private val addExistingRoomActivityResult = registerStartForActivityResult { _ ->
        viewModel.handle(SpaceDirectoryViewAction.Retry)
    }

    override fun addExistingRooms(spaceId: String) {
        addExistingRoomActivityResult.launch(SpaceManageActivity.newIntent(requireContext(), spaceId, ManageType.AddRooms))
    }

    override fun onUrlClicked(url: String, title: String): Boolean {
        permalinkHandler
                .launch(requireActivity(), url, null)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { managed ->
                    if (!managed) {
                        if (title.isValidUrl() && url.isValidUrl() && URL(title).host != URL(url).host) {
                            MaterialAlertDialogBuilder(requireActivity(), R.style.ThemeOverlay_Vector_MaterialAlertDialog_Destructive)
                                    .setTitle(R.string.external_link_confirmation_title)
                                    .setMessage(
                                            getString(R.string.external_link_confirmation_message, title, url)
                                                    .toSpannable()
                                                    .colorizeMatchingText(url, colorProvider.getColorFromAttribute(R.attr.vctr_content_tertiary))
                                                    .colorizeMatchingText(title, colorProvider.getColorFromAttribute(R.attr.vctr_content_tertiary))
                                    )
                                    .setPositiveButton(R.string._continue) { _, _ ->
                                        openUrlInExternalBrowser(requireContext(), url)
                                    }
                                    .setNegativeButton(R.string.cancel, null)
                                    .show()
                        } else {
                            // Open in external browser, in a new Tab
                            openUrlInExternalBrowser(requireContext(), url)
                        }
                    }
                }
                .disposeOnDestroyView()
        // In fact it is always managed
        return true
    }

    override fun onUrlLongClicked(url: String): Boolean {
        // nothing?
        return false
    }
//    override fun navigateToRoom(roomId: String) {
//        viewModel.handle(SpaceDirectoryViewAction.NavigateToRoom(roomId))
//    }
}
