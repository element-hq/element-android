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

package im.vector.app.features.call.dialpad

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import im.vector.app.R
import im.vector.app.core.extensions.addChildFragment
import im.vector.app.core.platform.VectorBaseBottomSheetDialogFragment
import im.vector.app.databinding.BottomSheetCallDialPadBinding
import im.vector.app.features.settings.VectorLocale

class CallDialPadBottomSheet : VectorBaseBottomSheetDialogFragment<BottomSheetCallDialPadBinding>() {

    companion object {

        private const val EXTRA_SHOW_ACTIONS = "EXTRA_SHOW_ACTIONS"

        fun newInstance(showActions: Boolean): CallDialPadBottomSheet {
            return CallDialPadBottomSheet().apply {
                arguments = Bundle().apply {
                    putBoolean(EXTRA_SHOW_ACTIONS, showActions)
                }
            }
        }
    }

    override val showExpanded = true

    var callback: DialPadFragment.Callback? = null
        set(value) {
            field = value
            setCallbackToFragment(callback)
        }

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): BottomSheetCallDialPadBinding {
        return BottomSheetCallDialPadBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (savedInstanceState == null) {
            val showActions = arguments?.getBoolean(EXTRA_SHOW_ACTIONS, false) ?: false
            DialPadFragment().apply {
                arguments = Bundle().apply {
                    putBoolean(DialPadFragment.EXTRA_ENABLE_DELETE, showActions)
                    putBoolean(DialPadFragment.EXTRA_ENABLE_OK, showActions)
                    putBoolean(DialPadFragment.EXTRA_CURSOR_VISIBLE, false)
                    putString(DialPadFragment.EXTRA_REGION_CODE, VectorLocale.applicationLocale.country)
                }
                callback = DialPadFragmentCallbackWrapper(this@CallDialPadBottomSheet.callback)
            }.also {
                addChildFragment(R.id.callDialPadFragmentContainer, it)
            }
        } else {
            setCallbackToFragment(callback)
        }
        views.callDialPadClose.debouncedClicks {
            dismiss()
        }
    }

    override fun onDestroyView() {
        setCallbackToFragment(null)
        super.onDestroyView()
    }

    private fun setCallbackToFragment(callback: DialPadFragment.Callback?) {
        if (!isAdded) return
        val dialPadFragment = childFragmentManager.findFragmentById(R.id.callDialPadFragmentContainer) as? DialPadFragment
        dialPadFragment?.callback = DialPadFragmentCallbackWrapper(callback)
    }

    private inner class DialPadFragmentCallbackWrapper(val callback: DialPadFragment.Callback?): DialPadFragment.Callback {

        override fun onOkClicked(formatted: String?, raw: String?) {
            callback?.onOkClicked(formatted, raw)
            dismiss()
        }
    }
}
