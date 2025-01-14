/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.billcarsonfr.jsonviewer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import com.airbnb.mvrx.Mavericks
import im.vector.lib.core.utils.compat.getParcelableCompat

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
        val args: JSonViewerFragmentArgs = arguments?.getParcelableCompat(Mavericks.KEY_ARG) ?: return
        if (savedInstanceState == null) {
            childFragmentManager.beginTransaction()
                    .replace(
                            R.id.fragmentContainer,
                            JSonViewerFragment.newInstance(
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
