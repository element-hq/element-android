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
import com.airbnb.mvrx.MvRx
import im.vector.app.R
import im.vector.app.core.extensions.commitTransaction
import im.vector.app.core.platform.VectorBaseActivity
import im.vector.app.databinding.ActivitySimpleBinding
import im.vector.app.features.spaces.preview.SpacePreviewArgs
import im.vector.app.features.spaces.preview.SpacePreviewFragment

class SpacePreviewActivity : VectorBaseActivity<ActivitySimpleBinding>() {

    lateinit var sharedActionViewModel: SpacePreviewSharedActionViewModel

    override fun getBinding(): ActivitySimpleBinding = ActivitySimpleBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedActionViewModel = viewModelProvider.get(SpacePreviewSharedActionViewModel::class.java)
        sharedActionViewModel
                .observe()
                .subscribe { action ->
                    when (action) {
                        SpacePreviewSharedAction.DismissAction -> finish()
                        SpacePreviewSharedAction.ShowModalLoading -> showWaitingView()
                        SpacePreviewSharedAction.HideModalLoading -> hideWaitingView()
                        is SpacePreviewSharedAction.ShowErrorMessage -> action.error?.let { showSnackbar(it) }
                    }
                }
                .disposeOnDestroy()

        if (isFirstCreation()) {
            val simpleName = SpacePreviewFragment::class.java.simpleName
            val args = intent?.getParcelableExtra<SpacePreviewArgs>(MvRx.KEY_ARG)
            if (supportFragmentManager.findFragmentByTag(simpleName) == null) {
                supportFragmentManager.commitTransaction {
                    replace(R.id.simpleFragmentContainer,
                            SpacePreviewFragment::class.java,
                            Bundle().apply { this.putParcelable(MvRx.KEY_ARG, args) },
                            simpleName
                    )
                }
            }
        }
    }

    companion object {
        fun newIntent(context: Context, spaceIdOrAlias: String): Intent {
            return Intent(context, SpacePreviewActivity::class.java).apply {
                putExtra(MvRx.KEY_ARG, SpacePreviewArgs(spaceIdOrAlias))
            }
        }
    }
}
