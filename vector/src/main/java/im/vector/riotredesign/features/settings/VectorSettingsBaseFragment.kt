/*
 * Copyright 2019 New Vector Ltd
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

package im.vector.riotredesign.features.settings

import android.os.Bundle
import android.text.TextUtils
import android.view.View
import androidx.annotation.CallSuper
import im.vector.matrix.android.api.session.Session
import im.vector.riotredesign.R
import im.vector.riotredesign.core.platform.VectorPreferenceFragment
import im.vector.riotredesign.core.utils.toast
import org.koin.android.ext.android.inject

abstract class VectorSettingsBaseFragment : VectorPreferenceFragment() {

    private var mLoadingView: View? = null


    // members
    protected val mSession by inject<Session>()

    abstract val preferenceXmlRes: Int

    @CallSuper
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(preferenceXmlRes)

        bindPref()
    }

    override fun onResume() {
        super.onResume()

        // find the view from parent activity
        mLoadingView = activity?.findViewById(R.id.vector_settings_spinner_views)
    }

    abstract fun bindPref()


    /**
     * Display the loading view.
     */
    protected fun displayLoadingView() {
        // search the loading view from the upper view
        if (null == mLoadingView) {
            var parent = view

            while (parent != null && mLoadingView == null) {
                mLoadingView = parent.findViewById(R.id.vector_settings_spinner_views)
                parent = parent.parent as View
            }
        } else {
            mLoadingView?.visibility = View.VISIBLE
        }
    }

    /**
     * Hide the loading view.
     */
    protected fun hideLoadingView() {
        mLoadingView?.visibility = View.GONE
    }

    /**
     * Hide the loading view and refresh the preferences.
     *
     * @param refresh true to refresh the display
     */
    protected fun hideLoadingView(refresh: Boolean) {
        mLoadingView?.visibility = View.GONE

        if (refresh) {
            // TODO refreshDisplay()
        }
    }

    /**
     * A request has been processed.
     * Display a toast if there is a an error message
     *
     * @param errorMessage the error message
     */
    protected fun onCommonDone(errorMessage: String?) {
        if (!isAdded) {
            return
        }
        activity?.runOnUiThread {
            if (!TextUtils.isEmpty(errorMessage) && errorMessage != null) {
                activity?.toast(errorMessage!!)
            }
            hideLoadingView()
        }
    }


}