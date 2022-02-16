/*
 * Copyright (c) 2022 New Vector Ltd
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

package im.vector.app.features.onboarding.ftueauth

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import im.vector.app.core.di.ActiveSessionHolder
import im.vector.app.core.dialogs.GalleryOrCameraDialogHelper
import im.vector.app.core.extensions.singletonEntryPoint
import im.vector.app.core.resources.ColorProvider
import im.vector.app.databinding.FragmentFtueProfilePictureBinding
import im.vector.app.features.home.AvatarRenderer
import im.vector.app.features.onboarding.OnboardingAction
import im.vector.app.features.onboarding.OnboardingViewState
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.util.MatrixItem
import javax.inject.Inject

class FtueAuthChooseProfilePictureFragment @Inject constructor(
        private val activeSessionHolder: ActiveSessionHolder,
        colorProvider: ColorProvider
) : AbstractFtueAuthFragment<FragmentFtueProfilePictureBinding>(), GalleryOrCameraDialogHelper.Listener {

    private val galleryOrCameraDialogHelper = GalleryOrCameraDialogHelper(this, colorProvider)
    private val avatarRenderer: AvatarRenderer by lazy { requireContext().singletonEntryPoint().avatarRenderer() }

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentFtueProfilePictureBinding {
        return FragmentFtueProfilePictureBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
    }

    private fun setupViews() {
        views.profilePictureSubmit.isEnabled = false

        lifecycleScope.launch {
            val session = activeSessionHolder.getActiveSession()
            val matrixItem = MatrixItem.UserItem(
                    id = session.myUserId,
                    displayName = session.getDisplayName(session.myUserId).getOrElse { "" }
            )
            avatarRenderer.render(matrixItem, localUri = null, imageView = views.profilePictureView)
        }

        views.profilePictureView.setOnClickListener {
            galleryOrCameraDialogHelper.show()
        }



        views.profilePictureSkip.setOnClickListener { viewModel.handle(OnboardingAction.UpdateProfilePictureSkipped) }
    }

    override fun updateWithState(state: OnboardingViewState) {
        views.profilePictureSubmit.isEnabled = false
        views.profilePictureSubmit.setOnClickListener {
            // TODO
        }
    }

    override fun onImageReady(uri: Uri?) {
        views.profilePictureSubmit.isEnabled = uri != null

        if (uri != null) {
            lifecycleScope.launch {
                val session = activeSessionHolder.getActiveSession()
                val matrixItem = MatrixItem.UserItem(
                        id = session.myUserId,
                        displayName = session.getDisplayName(session.myUserId).getOrElse { "" }
                )
                avatarRenderer.render(matrixItem, localUri = uri, imageView = views.profilePictureView)
            }
            //TODO update state
        } else {
            Toast.makeText(requireContext(), "Cannot retrieve cropped value", Toast.LENGTH_SHORT).show()
        }
    }

    override fun resetViewModel() {
        // Nothing to do
    }
}
