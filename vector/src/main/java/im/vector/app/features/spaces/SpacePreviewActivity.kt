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

package im.vector.app.features.spaces

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.airbnb.mvrx.Mavericks
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.core.extensions.replaceFragment
import im.vector.app.core.platform.VectorBaseActivity
import im.vector.app.databinding.ActivitySimpleBinding
import im.vector.app.features.spaces.preview.SpacePreviewArgs
import im.vector.app.features.spaces.preview.SpacePreviewFragment
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@AndroidEntryPoint
class SpacePreviewActivity : VectorBaseActivity<ActivitySimpleBinding>() {

    lateinit var sharedActionViewModel: SpacePreviewSharedActionViewModel

    override fun getBinding(): ActivitySimpleBinding = ActivitySimpleBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedActionViewModel = viewModelProvider.get(SpacePreviewSharedActionViewModel::class.java)
        sharedActionViewModel
                .stream()
                .onEach { action ->
                    when (action) {
                        SpacePreviewSharedAction.DismissAction       -> finish()
                        SpacePreviewSharedAction.ShowModalLoading    -> showWaitingView()
                        SpacePreviewSharedAction.HideModalLoading    -> hideWaitingView()
                        is SpacePreviewSharedAction.ShowErrorMessage -> action.error?.let { showSnackbar(it) }
                    }
                }
                .launchIn(lifecycleScope)

        if (isFirstCreation()) {
            val args = intent?.getParcelableExtra<SpacePreviewArgs>(Mavericks.KEY_ARG)
            replaceFragment(
                    views.simpleFragmentContainer,
                    SpacePreviewFragment::class.java,
                    args
            )
        }
    }

    companion object {
        fun newIntent(context: Context, spaceIdOrAlias: String): Intent {
            return Intent(context, SpacePreviewActivity::class.java).apply {
                putExtra(Mavericks.KEY_ARG, SpacePreviewArgs(spaceIdOrAlias))
            }
        }
    }
}
