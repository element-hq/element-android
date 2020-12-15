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

import android.view.LayoutInflater
import androidx.annotation.CallSuper
import androidx.core.view.isGone
import androidx.core.view.isVisible
import im.vector.app.R
import im.vector.app.core.di.ScreenComponent
import im.vector.app.core.extensions.hideKeyboard
import im.vector.app.databinding.ActivityBinding

import org.matrix.android.sdk.api.session.Session
import javax.inject.Inject

/**
 * Simple activity with a toolbar, a waiting overlay, and a fragment container and a session.
 */
abstract class SimpleFragmentActivity : VectorBaseActivity<ActivityBinding>() {

    override fun getBinding() = ActivityBinding.inflate(layoutInflater)

    @Inject lateinit var session: Session

    @CallSuper
    override fun injectWith(injector: ScreenComponent) {
        session = injector.activeSessionHolder().getActiveSession()
    }

    override fun initUiAndData() {
        configureToolbar(views.toolbar)
        waitingView = views.overlayWaitingView.waitingView
    }

    /**
     * Displays a progress indicator with a message to the user.
     * Blocks user interactions.
     */
    fun updateWaitingView(data: WaitingViewData?) {
        data?.let {
            views.overlayWaitingView.waitingStatusText.text = data.message

            if (data.progress != null && data.progressTotal != null) {
                views.overlayWaitingView.waitingHorizontalProgress.isIndeterminate = false
                views.overlayWaitingView.waitingHorizontalProgress.progress = data.progress
                views.overlayWaitingView.waitingHorizontalProgress.max = data.progressTotal
                views.overlayWaitingView.waitingHorizontalProgress.isVisible = true
                views.overlayWaitingView.waitingCircularProgress.isVisible = false
            } else if (data.isIndeterminate) {
                views.overlayWaitingView.waitingHorizontalProgress.isIndeterminate = true
                views.overlayWaitingView.waitingHorizontalProgress.isVisible = true
                views.overlayWaitingView.waitingCircularProgress.isVisible = false
            } else {
                views.overlayWaitingView.waitingHorizontalProgress.isVisible = false
                views.overlayWaitingView.waitingCircularProgress.isVisible = true
            }

            showWaitingView()
        } ?: run {
            hideWaitingView()
        }
    }

    override fun showWaitingView() {
        hideKeyboard()
        views.overlayWaitingView.waitingStatusText.isGone = views.overlayWaitingView.waitingStatusText.text.isNullOrBlank()
        super.showWaitingView()
    }

    override fun hideWaitingView() {
        views.overlayWaitingView.waitingStatusText.text = null
        views.overlayWaitingView.waitingStatusText.isGone = true
        views.overlayWaitingView.waitingHorizontalProgress.progress = 0
        views.overlayWaitingView.waitingHorizontalProgress.isVisible = false
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
