/*
 * Copyright 2020 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package im.vector.app.features.roommemberprofile.devices

import android.content.DialogInterface
import android.os.Bundle
import android.os.Parcelable
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.R
import im.vector.app.core.extensions.commitTransaction
import im.vector.app.core.platform.VectorBaseBottomSheetDialogFragment
import im.vector.app.databinding.BottomSheetWithFragmentsBinding
import im.vector.app.features.crypto.verification.VerificationBottomSheet
import kotlinx.parcelize.Parcelize
import kotlin.reflect.KClass

@AndroidEntryPoint
class DeviceListBottomSheet :
        VectorBaseBottomSheetDialogFragment<BottomSheetWithFragmentsBinding>() {

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): BottomSheetWithFragmentsBinding {
        return BottomSheetWithFragmentsBinding.inflate(inflater, container, false)
    }

    private val viewModel: DeviceListBottomSheetViewModel by fragmentViewModel(DeviceListBottomSheetViewModel::class)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.observeViewEvents {
            when (it) {
                is DeviceListBottomSheetViewEvents.Verify -> {
                    VerificationBottomSheet.withArgs(
                            roomId = null,
                            otherUserId = it.userId,
                            transactionId = it.txID
                    ).show(requireActivity().supportFragmentManager, "REQPOP")
                }
            }
        }
    }

    private val onKeyListener = DialogInterface.OnKeyListener { _, keyCode, _ ->
        withState(viewModel) {
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                if (it.selectedDevice != null) {
                    viewModel.handle(DeviceListAction.DeselectDevice)
                    return@withState true
                } else {
                    return@withState false
                }
            }
            return@withState false
        }
    }

    override fun onResume() {
        super.onResume()
        dialog?.setOnKeyListener(onKeyListener)
    }

    override fun onPause() {
        super.onPause()
        dialog?.setOnKeyListener(null)
    }

    override fun invalidate() = withState(viewModel) {
        super.invalidate()
        if (it.selectedDevice == null) {
            showFragment(DeviceListFragment::class, arguments ?: Bundle())
        } else {
            showFragment(DeviceTrustInfoActionFragment::class, arguments ?: Bundle())
        }
    }

    private fun showFragment(fragmentClass: KClass<out Fragment>, bundle: Bundle) {
        if (childFragmentManager.findFragmentByTag(fragmentClass.simpleName) == null) {
            childFragmentManager.commitTransaction {
                replace(R.id.bottomSheetFragmentContainer,
                        fragmentClass.java,
                        bundle,
                        fragmentClass.simpleName
                )
            }
        }
    }

    @Parcelize
    data class Args(
            val userId: String,
            val allowDeviceAction: Boolean
    ) : Parcelable

    companion object {
        fun newInstance(userId: String, allowDeviceAction: Boolean = true): DeviceListBottomSheet {
            return DeviceListBottomSheet().apply {
                setArguments(Args(userId, allowDeviceAction))
            }
        }
    }
}
