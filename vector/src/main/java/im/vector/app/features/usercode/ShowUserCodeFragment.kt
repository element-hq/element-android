/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.usercode

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.airbnb.mvrx.activityViewModel
import com.airbnb.mvrx.withState
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.core.extensions.setTextOrHide
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.core.utils.PERMISSIONS_FOR_TAKING_PHOTO
import im.vector.app.core.utils.checkPermissions
import im.vector.app.core.utils.registerForPermissionsResult
import im.vector.app.core.utils.startSharePlainTextIntent
import im.vector.app.databinding.FragmentUserCodeShowBinding
import im.vector.app.features.home.AvatarRenderer
import javax.inject.Inject

@AndroidEntryPoint
class ShowUserCodeFragment :
        VectorBaseFragment<FragmentUserCodeShowBinding>() {

    @Inject lateinit var avatarRenderer: AvatarRenderer

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentUserCodeShowBinding {
        return FragmentUserCodeShowBinding.inflate(inflater, container, false)
    }

    val sharedViewModel: UserCodeSharedViewModel by activityViewModel()

    private val openCameraActivityResultLauncher = registerForPermissionsResult { allGranted, deniedPermanently ->
        if (allGranted) {
            doOpenQRCodeScanner()
        } else {
            sharedViewModel.handle(UserCodeActions.CameraPermissionNotGranted(deniedPermanently))
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar(views.showUserCodeToolBar)
                .allowBack(useCross = true)

        views.showUserCodeScanButton.debouncedClicks {
            if (checkPermissions(PERMISSIONS_FOR_TAKING_PHOTO, requireActivity(), openCameraActivityResultLauncher)) {
                doOpenQRCodeScanner()
            }
        }
        views.showUserCodeShareButton.debouncedClicks {
            sharedViewModel.handle(UserCodeActions.ShareByText)
        }

        sharedViewModel.observeViewEvents {
            if (it is UserCodeShareViewEvents.SharePlainText) {
                startSharePlainTextIntent(
                        context = requireContext(),
                        activityResultLauncher = null,
                        chooserTitle = it.title,
                        text = it.text,
                        extraTitle = it.richPlainText
                )
            }
        }
    }

    private fun doOpenQRCodeScanner() {
        sharedViewModel.handle(UserCodeActions.SwitchMode(UserCodeState.Mode.SCAN))
    }

    override fun invalidate() = withState(sharedViewModel) { state ->
        state.matrixItem?.let { avatarRenderer.render(it, views.showUserCodeAvatar) }
        state.shareLink?.let { views.showUserCodeQRImage.setData(it) }
        views.showUserCodeCardNameText.setTextOrHide(state.matrixItem?.displayName)
        views.showUserCodeCardUserIdText.setTextOrHide(state.matrixItem?.id)
    }
}
