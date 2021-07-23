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

package im.vector.app.features.usercode

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.airbnb.mvrx.MvRx
import com.airbnb.mvrx.viewModel
import com.airbnb.mvrx.withState
import im.vector.app.R
import im.vector.app.core.di.ScreenComponent
import im.vector.app.core.extensions.commitTransaction
import im.vector.app.core.extensions.exhaustive
import im.vector.app.core.platform.VectorBaseActivity
import im.vector.app.core.utils.onPermissionDeniedSnackbar
import im.vector.app.databinding.ActivitySimpleBinding
import im.vector.app.features.matrixto.MatrixToBottomSheet
import kotlinx.parcelize.Parcelize
import javax.inject.Inject
import kotlin.reflect.KClass

class UserCodeActivity : VectorBaseActivity<ActivitySimpleBinding>(),
        UserCodeSharedViewModel.Factory,
        MatrixToBottomSheet.InteractionListener {

    @Inject lateinit var viewModelFactory: UserCodeSharedViewModel.Factory

    val sharedViewModel: UserCodeSharedViewModel by viewModel()

    @Parcelize
    data class Args(
            val userId: String
    ) : Parcelable

    override fun getBinding() = ActivitySimpleBinding.inflate(layoutInflater)

    override fun getCoordinatorLayout() = views.coordinatorLayout

    override fun injectWith(injector: ScreenComponent) {
        injector.inject(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (isFirstCreation()) {
            // should be there early for shared element transition
            showFragment(ShowUserCodeFragment::class, Bundle.EMPTY)
        }

        sharedViewModel.selectSubscribe(this, UserCodeState::mode) { mode ->
            when (mode) {
                UserCodeState.Mode.SHOW      -> showFragment(ShowUserCodeFragment::class, Bundle.EMPTY)
                UserCodeState.Mode.SCAN      -> showFragment(ScanUserCodeFragment::class, Bundle.EMPTY)
                is UserCodeState.Mode.RESULT -> {
                    showFragment(ShowUserCodeFragment::class, Bundle.EMPTY)
                    MatrixToBottomSheet.withLink(mode.rawLink, this).show(supportFragmentManager, "MatrixToBottomSheet")
                }
            }
        }

        sharedViewModel.observeViewEvents {
            when (it) {
                UserCodeShareViewEvents.Dismiss                       -> ActivityCompat.finishAfterTransition(this)
                UserCodeShareViewEvents.ShowWaitingScreen             -> views.simpleActivityWaitingView.isVisible = true
                UserCodeShareViewEvents.HideWaitingScreen             -> views.simpleActivityWaitingView.isVisible = false
                is UserCodeShareViewEvents.ToastMessage               -> Toast.makeText(this, it.message, Toast.LENGTH_LONG).show()
                is UserCodeShareViewEvents.NavigateToRoom             -> navigator.openRoom(this, it.roomId)
                is UserCodeShareViewEvents.CameraPermissionNotGranted -> {
                    if (it.deniedPermanently) {
                        onPermissionDeniedSnackbar(R.string.permissions_denied_qr_code)
                    }
                }
                else                                                  -> {
                }
            }
        }
    }

    private fun showFragment(fragmentClass: KClass<out Fragment>, bundle: Bundle) {
        if (supportFragmentManager.findFragmentByTag(fragmentClass.simpleName) == null) {
            supportFragmentManager.commitTransaction {
                setCustomAnimations(R.anim.fade_in, R.anim.fade_out, R.anim.fade_in, R.anim.fade_out)
                replace(R.id.simpleFragmentContainer,
                        fragmentClass.java,
                        bundle,
                        fragmentClass.simpleName
                )
            }
        }
    }

    override fun navigateToRoom(roomId: String) {
        navigator.openRoom(this, roomId)
    }

    override fun onBackPressed() = withState(sharedViewModel) {
        when (it.mode) {
            UserCodeState.Mode.SHOW -> super.onBackPressed()
            is UserCodeState.Mode.RESULT,
            UserCodeState.Mode.SCAN -> sharedViewModel.handle(UserCodeActions.SwitchMode(UserCodeState.Mode.SHOW))
        }.exhaustive
    }

    override fun create(initialState: UserCodeState) =
            viewModelFactory.create(initialState)

    companion object {
        fun newIntent(context: Context, userId: String): Intent {
            return Intent(context, UserCodeActivity::class.java).apply {
                putExtra(MvRx.KEY_ARG, Args(userId))
            }
        }
    }
}
