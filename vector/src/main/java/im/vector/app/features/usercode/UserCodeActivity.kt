/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.usercode

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.airbnb.mvrx.Mavericks
import com.airbnb.mvrx.viewModel
import com.airbnb.mvrx.withState
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.core.extensions.replaceFragment
import im.vector.app.core.platform.VectorBaseActivity
import im.vector.app.core.utils.onPermissionDeniedSnackbar
import im.vector.app.databinding.ActivitySimpleBinding
import im.vector.app.features.analytics.plan.ViewRoom
import im.vector.app.features.matrixto.MatrixToBottomSheet
import im.vector.app.features.matrixto.OriginOfMatrixTo
import im.vector.app.features.qrcode.QrCodeScannerEvents
import im.vector.app.features.qrcode.QrCodeScannerFragment
import im.vector.app.features.qrcode.QrCodeScannerViewModel
import im.vector.app.features.qrcode.QrScannerArgs
import im.vector.lib.strings.CommonStrings
import kotlinx.parcelize.Parcelize
import kotlin.reflect.KClass

@AndroidEntryPoint
class UserCodeActivity : VectorBaseActivity<ActivitySimpleBinding>(),
        MatrixToBottomSheet.InteractionListener {

    val sharedViewModel: UserCodeSharedViewModel by viewModel()
    private val qrViewModel: QrCodeScannerViewModel by viewModel()

    @Parcelize
    data class Args(
            val userId: String
    ) : Parcelable

    override fun getBinding() = ActivitySimpleBinding.inflate(layoutInflater)

    override fun getCoordinatorLayout() = views.coordinatorLayout

    private val fragmentLifecycleCallbacks = object : FragmentManager.FragmentLifecycleCallbacks() {
        override fun onFragmentResumed(fm: FragmentManager, f: Fragment) {
            if (f is MatrixToBottomSheet) {
                f.interactionListener = this@UserCodeActivity
            }
            super.onFragmentResumed(fm, f)
        }

        override fun onFragmentPaused(fm: FragmentManager, f: Fragment) {
            if (f is MatrixToBottomSheet) {
                f.interactionListener = null
            }
            super.onFragmentPaused(fm, f)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportFragmentManager.registerFragmentLifecycleCallbacks(fragmentLifecycleCallbacks, false)

        if (isFirstCreation()) {
            // should be there early for shared element transition
            showFragment(ShowUserCodeFragment::class, Bundle.EMPTY)
        }

        sharedViewModel.onEach(UserCodeState::mode) { mode ->
            when (mode) {
                UserCodeState.Mode.SHOW -> showFragment(ShowUserCodeFragment::class)
                UserCodeState.Mode.SCAN -> {
                    val args = QrScannerArgs(showExtraButtons = true, CommonStrings.user_code_scan)
                    showFragment(QrCodeScannerFragment::class, args)
                }
                is UserCodeState.Mode.RESULT -> {
                    showFragment(ShowUserCodeFragment::class)
                    MatrixToBottomSheet.withLink(mode.rawLink, OriginOfMatrixTo.USER_CODE).show(supportFragmentManager, "MatrixToBottomSheet")
                }
            }
        }

        sharedViewModel.observeViewEvents {
            when (it) {
                UserCodeShareViewEvents.Dismiss -> ActivityCompat.finishAfterTransition(this)
                UserCodeShareViewEvents.ShowWaitingScreen -> views.simpleActivityWaitingView.isVisible = true
                UserCodeShareViewEvents.HideWaitingScreen -> views.simpleActivityWaitingView.isVisible = false
                is UserCodeShareViewEvents.ToastMessage -> Toast.makeText(this, it.message, Toast.LENGTH_LONG).show()
                is UserCodeShareViewEvents.NavigateToRoom -> navigator.openRoom(this, it.roomId)
                is UserCodeShareViewEvents.CameraPermissionNotGranted -> {
                    if (it.deniedPermanently) {
                        onPermissionDeniedSnackbar(CommonStrings.permissions_denied_qr_code)
                    }
                }
                else -> {
                }
            }
        }

        qrViewModel.observeViewEvents {
            when (it) {
                is QrCodeScannerEvents.CodeParsed -> {
                    sharedViewModel.handle(UserCodeActions.DecodedQRCode(it.result))
                }
                QrCodeScannerEvents.SwitchMode -> {
                    sharedViewModel.handle(UserCodeActions.SwitchMode(UserCodeState.Mode.SHOW))
                }
                is QrCodeScannerEvents.ParseFailed -> {
                    Toast.makeText(this, CommonStrings.qr_code_not_scanned, Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }
    }

    override fun onDestroy() {
        supportFragmentManager.unregisterFragmentLifecycleCallbacks(fragmentLifecycleCallbacks)
        super.onDestroy()
    }

    private fun showFragment(fragmentClass: KClass<out Fragment>, params: Parcelable? = null) {
        if (supportFragmentManager.findFragmentByTag(fragmentClass.simpleName) == null) {
            replaceFragment(views.simpleFragmentContainer, fragmentClass.java, params, fragmentClass.simpleName, useCustomAnimation = true)
        }
    }

    override fun mxToBottomSheetNavigateToRoom(roomId: String, trigger: ViewRoom.Trigger?) {
        navigator.openRoom(this, roomId, trigger = trigger)
    }

    override fun mxToBottomSheetSwitchToSpace(spaceId: String) {}

    @Suppress("OVERRIDE_DEPRECATION")
    override fun onBackPressed() = withState(sharedViewModel) {
        when (it.mode) {
            UserCodeState.Mode.SHOW -> {
                @Suppress("DEPRECATION")
                super.onBackPressed()
            }
            is UserCodeState.Mode.RESULT,
            UserCodeState.Mode.SCAN -> sharedViewModel.handle(UserCodeActions.SwitchMode(UserCodeState.Mode.SHOW))
        }
    }

    companion object {
        fun newIntent(context: Context, userId: String): Intent {
            return Intent(context, UserCodeActivity::class.java).apply {
                putExtra(Mavericks.KEY_ARG, Args(userId))
            }
        }
    }
}
