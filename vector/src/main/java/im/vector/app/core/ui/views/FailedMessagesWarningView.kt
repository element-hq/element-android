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

package im.vector.app.core.ui.views

import android.content.Context
import android.util.AttributeSet
import androidx.constraintlayout.widget.ConstraintLayout
import im.vector.app.R
import im.vector.app.databinding.ViewFailedMessagesWarningBinding

class FailedMessagesWarningView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    interface Callback {
        fun onDeleteAllClicked()
        fun onRetryClicked()
    }

    var callback: Callback? = null

    private lateinit var views: ViewFailedMessagesWarningBinding

    init {
        setupViews()
    }

    private fun setupViews() {
        inflate(context, R.layout.view_failed_messages_warning, this)
        views = ViewFailedMessagesWarningBinding.bind(this)

        views.failedMessagesDeleteAllButton.setOnClickListener { callback?.onDeleteAllClicked() }
        views.failedMessagesRetryButton.setOnClickListener { callback?.onRetryClicked() }
    }
}
