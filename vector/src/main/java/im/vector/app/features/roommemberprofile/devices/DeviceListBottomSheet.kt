/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
            // nop
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
                replace(
                        R.id.bottomSheetFragmentContainer,
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
    ) : Parcelable

    companion object {
        fun newInstance(userId: String): DeviceListBottomSheet {
            return DeviceListBottomSheet().apply {
                setArguments(Args(userId))
            }
        }
    }
}
