/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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

    @Suppress("OVERRIDE_DEPRECATION")
    override fun onBackPressed() {
        if (waitingView!!.isVisible) {
            // ignore
            return
        }
        @Suppress("DEPRECATION")
        super.onBackPressed()
    }
}
