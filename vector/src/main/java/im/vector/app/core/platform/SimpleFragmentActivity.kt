/*
 * Copyright 2019 New Vector Ltd
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
package im.vector.app.core.platform

import androidx.core.view.isGone
import androidx.core.view.isVisible
import im.vector.app.core.extensions.hideKeyboard
import im.vector.app.databinding.ActivityBinding

/**
 * Simple activity with a toolbar, a waiting overlay, and a fragment container and a session.
 */
abstract class SimpleFragmentActivity : VectorBaseActivity<ActivityBinding>() {

    final override fun getBinding() = ActivityBinding.inflate(layoutInflater)

    final override fun getCoordinatorLayout() = views.coordinatorLayout

    override fun initUiAndData() {
        setupToolbar(views.toolbar)
                .allowBack(true)
        waitingView = views.waitingView.waitingView
    }

    /**
     * Displays a progress indicator with a message to the user.
     * Blocks user interactions.
     */
    fun updateWaitingView(data: WaitingViewData?) {
        data?.let {
            views.waitingView.waitingStatusText.text = data.message

            if (data.progress != null && data.progressTotal != null) {
                views.waitingView.waitingHorizontalProgress.isIndeterminate = false
                views.waitingView.waitingHorizontalProgress.progress = data.progress
                views.waitingView.waitingHorizontalProgress.max = data.progressTotal
                views.waitingView.waitingHorizontalProgress.isVisible = true
                views.waitingView.waitingCircularProgress.isVisible = false
            } else if (data.isIndeterminate) {
                views.waitingView.waitingHorizontalProgress.isIndeterminate = true
                views.waitingView.waitingHorizontalProgress.isVisible = true
                views.waitingView.waitingCircularProgress.isVisible = false
            } else {
                views.waitingView.waitingHorizontalProgress.isVisible = false
                views.waitingView.waitingCircularProgress.isVisible = true
            }

            showWaitingView()
        } ?: run {
            hideWaitingView()
        }
    }

    override fun showWaitingView(text: String?) {
        hideKeyboard()
        views.waitingView.waitingStatusText.isGone = views.waitingView.waitingStatusText.text.isNullOrBlank()
        super.showWaitingView(text)
    }

    override fun hideWaitingView() {
        views.waitingView.waitingStatusText.text = null
        views.waitingView.waitingStatusText.isGone = true
        views.waitingView.waitingHorizontalProgress.progress = 0
        views.waitingView.waitingHorizontalProgress.isVisible = false
        super.hideWaitingView()
    }

    override fun onBackPressed() {
        if (waitingView!!.isVisible) {
            // ignore
            return
        }
        super.onBackPressed()
    }
}
