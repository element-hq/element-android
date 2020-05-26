/*
 * Copyright (c) 2020 New Vector Ltd
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

package im.vector.riotx.features.widgets

import android.content.Context
import android.content.Intent
import androidx.appcompat.widget.Toolbar
import im.vector.matrix.android.api.session.events.model.Content
import im.vector.riotx.R
import im.vector.riotx.core.extensions.addFragment
import im.vector.riotx.core.platform.ToolbarConfigurable
import im.vector.riotx.core.platform.VectorBaseActivity
import java.io.Serializable

class WidgetActivity : VectorBaseActivity(), ToolbarConfigurable {

    companion object {

        private const val EXTRA_RESULT = "EXTRA_RESULT"
        private const val EXTRA_FRAGMENT_ARGS = "EXTRA_FRAGMENT_ARGS"

        fun newIntent(context: Context, args: WidgetArgs): Intent {
            return Intent(context, WidgetActivity::class.java).apply {
                putExtra(EXTRA_FRAGMENT_ARGS, args)
            }
        }

        @Suppress("UNCHECKED_CAST")
        fun getOutput(intent: Intent): Content? {
            return intent.extras?.getSerializable(EXTRA_RESULT) as? Content
        }

        fun createResultIntent(content: Content): Intent {
            return Intent().apply {
                putExtra(EXTRA_RESULT, content as Serializable)
            }
        }
    }

    override fun getLayoutRes() = R.layout.activity_simple

    override fun initUiAndData() {
        if (isFirstCreation()) {
            val fragmentArgs: WidgetArgs = intent?.extras?.getParcelable(EXTRA_FRAGMENT_ARGS)
                    ?: return
            addFragment(R.id.simpleFragmentContainer, WidgetFragment::class.java, fragmentArgs)
        }
    }

    override fun configure(toolbar: Toolbar) {
        configureToolbar(toolbar)
    }
}
