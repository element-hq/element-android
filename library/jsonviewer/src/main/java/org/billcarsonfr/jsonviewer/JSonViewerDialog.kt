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

package org.billcarsonfr.jsonviewer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import com.airbnb.mvrx.Mavericks

class JSonViewerDialog : DialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_dialog_jv, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val args: JSonViewerFragmentArgs = arguments?.getParcelable(Mavericks.KEY_ARG) ?: return
        if (savedInstanceState == null) {
            childFragmentManager.beginTransaction()
                .replace(
                    R.id.fragmentContainer, JSonViewerFragment.newInstance(
                        args.jsonString,
                        args.defaultOpenDepth,
                        true,
                        args.styleProvider
                    )
                )
                .commitNow()
        }
    }

    override fun onResume() {
        super.onResume()
        // Get existing layout params for the window
        val params = dialog?.window?.attributes
        // Assign window properties to fill the parent
        params?.width = WindowManager.LayoutParams.MATCH_PARENT
        params?.height = WindowManager.LayoutParams.MATCH_PARENT
        dialog?.window?.attributes = params
    }

    companion object {
        fun newInstance(
            jsonString: String,
            initialOpenDepth: Int = -1,
            styleProvider: JSonViewerStyleProvider? = null
        ): JSonViewerDialog {
            val args = Bundle()
            val parcelableArgs =
                JSonViewerFragmentArgs(jsonString, initialOpenDepth, false, styleProvider)
            args.putParcelable(Mavericks.KEY_ARG, parcelableArgs)
            return JSonViewerDialog().apply { arguments = args }
        }
    }
}
