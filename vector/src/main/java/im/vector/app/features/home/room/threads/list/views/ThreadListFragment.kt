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

package im.vector.app.features.home.room.threads.list.views

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.airbnb.mvrx.args
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import im.vector.app.R
import im.vector.app.core.extensions.cleanup
import im.vector.app.core.extensions.configureWith
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.databinding.FragmentThreadListBinding
import im.vector.app.features.home.AvatarRenderer
import im.vector.app.features.home.room.breadcrumbs.BreadcrumbsAnimator
import im.vector.app.features.home.room.breadcrumbs.BreadcrumbsViewModel
import im.vector.app.features.home.room.detail.RoomDetailSharedActionViewModel
import im.vector.app.features.home.room.threads.arguments.ThreadListArgs
import im.vector.app.features.home.room.threads.list.viewmodel.ThreadSummaryController
import im.vector.app.features.home.room.threads.list.viewmodel.ThreadSummaryViewModel
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.util.MatrixItem
import javax.inject.Inject

class ThreadListFragment @Inject constructor(
        private val session: Session,
        private val avatarRenderer: AvatarRenderer,
        private val threadSummaryController: ThreadSummaryController,
        val threadSummaryViewModelFactory: ThreadSummaryViewModel.Factory
) : VectorBaseFragment<FragmentThreadListBinding>() {

    private val threadSummaryViewModel: ThreadSummaryViewModel by fragmentViewModel()

    private val threadListArgs: ThreadListArgs by args()

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentThreadListBinding {
        return FragmentThreadListBinding.inflate(inflater, container, false)
    }

    override fun getMenuRes() = R.menu.menu_thread_list

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initToolbar()
        views.threadListRecyclerView.configureWith(threadSummaryController, BreadcrumbsAnimator(), hasFixedSize = false)
//        threadSummaryController.listener = this
    }

    override fun onDestroyView() {
        views.threadListRecyclerView.cleanup()
//        breadcrumbsController.listener = null
        super.onDestroyView()
    }
    private fun initToolbar(){
        setupToolbar(views.threadListToolbar)
        renderToolbar()
    }

    override fun invalidate() = withState(threadSummaryViewModel) { state ->
        threadSummaryController.update(state)
    }

    private fun renderToolbar() {
        views.includeThreadListToolbar.roomToolbarThreadConstraintLayout.isVisible = true
            val matrixItem = MatrixItem.RoomItem(threadListArgs.roomId, threadListArgs.displayName, threadListArgs.avatarUrl)
            avatarRenderer.render(matrixItem, views.includeThreadListToolbar.roomToolbarThreadImageView)
            views.includeThreadListToolbar.roomToolbarThreadSubtitleTextView.text = threadListArgs.displayName
    }
}
