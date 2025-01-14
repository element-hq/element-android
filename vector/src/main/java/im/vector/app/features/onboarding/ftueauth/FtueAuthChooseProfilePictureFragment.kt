/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.onboarding.ftueauth

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isInvisible
import com.airbnb.mvrx.withState
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.R
import im.vector.app.core.dialogs.GalleryOrCameraDialogHelper
import im.vector.app.core.dialogs.GalleryOrCameraDialogHelperFactory
import im.vector.app.databinding.FragmentFtueProfilePictureBinding
import im.vector.app.features.home.AvatarRenderer
import im.vector.app.features.onboarding.OnboardingAction
import im.vector.app.features.onboarding.OnboardingViewEvents
import im.vector.app.features.onboarding.OnboardingViewState
import org.matrix.android.sdk.api.util.MatrixItem
import javax.inject.Inject

@AndroidEntryPoint
class FtueAuthChooseProfilePictureFragment :
        AbstractFtueAuthFragment<FragmentFtueProfilePictureBinding>(),
        GalleryOrCameraDialogHelper.Listener {

    @Inject lateinit var galleryOrCameraDialogHelperFactory: GalleryOrCameraDialogHelperFactory
    @Inject lateinit var avatarRenderer: AvatarRenderer

    private lateinit var galleryOrCameraDialogHelper: GalleryOrCameraDialogHelper

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentFtueProfilePictureBinding {
        return FragmentFtueProfilePictureBinding.inflate(inflater, container, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        galleryOrCameraDialogHelper = galleryOrCameraDialogHelperFactory.create(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
    }

    private fun setupViews() {
        views.profilePictureToolbar.setNavigationOnClickListener {
            viewModel.handle(OnboardingAction.PostViewEvent(OnboardingViewEvents.OnBack))
        }
        views.changeProfilePictureButton.debouncedClicks { galleryOrCameraDialogHelper.show() }
        views.profilePictureView.debouncedClicks { galleryOrCameraDialogHelper.show() }

        views.profilePictureSubmit.debouncedClicks {
            withState(viewModel) {
                viewModel.handle(OnboardingAction.SaveSelectedProfilePicture)
            }
        }

        views.profilePictureSkip.debouncedClicks { viewModel.handle(OnboardingAction.UpdateProfilePictureSkipped) }
    }

    override fun updateWithState(state: OnboardingViewState) {
        views.profilePictureToolbar.isInvisible = !state.personalizationState.supportsChangingDisplayName

        val hasSetPicture = state.personalizationState.selectedPictureUri != null
        views.profilePictureSubmit.isEnabled = hasSetPicture
        views.changeProfilePictureIcon.setImageResource(if (hasSetPicture) R.drawable.ic_edit else R.drawable.ic_camera_plain)

        val matrixItem = MatrixItem.UserItem(
                id = state.personalizationState.userId,
                displayName = state.personalizationState.displayName.orEmpty()
        )
        avatarRenderer.render(matrixItem, localUri = state.personalizationState.selectedPictureUri, imageView = views.profilePictureView)
    }

    override fun onImageReady(uri: Uri?) {
        if (uri == null) {
            Toast.makeText(requireContext(), "Cannot retrieve cropped value", Toast.LENGTH_SHORT).show()
        } else {
            viewModel.handle(OnboardingAction.ProfilePictureSelected(uri))
        }
    }

    override fun resetViewModel() {
        // Nothing to do
    }

    override fun onBackPressed(toolbarButton: Boolean): Boolean {
        return when (withState(viewModel) { it.personalizationState.supportsChangingDisplayName }) {
            true -> super.onBackPressed(toolbarButton)
            false -> {
                viewModel.handle(OnboardingAction.PostViewEvent(OnboardingViewEvents.OnTakeMeHome))
                true
            }
        }
    }
}
