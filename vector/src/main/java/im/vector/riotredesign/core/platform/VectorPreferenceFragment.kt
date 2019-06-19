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

package im.vector.riotredesign.core.platform

import androidx.annotation.CallSuper
import androidx.preference.PreferenceFragmentCompat
import im.vector.riotredesign.R
import im.vector.riotredesign.core.utils.toast
import timber.log.Timber

abstract class VectorPreferenceFragment : PreferenceFragmentCompat() {

    val vectorActivity: VectorBaseActivity by lazy {
        activity as VectorBaseActivity
    }

    abstract var titleRes: Int

    /* ==========================================================================================
     * Life cycle
     * ========================================================================================== */

    @CallSuper
    override fun onResume() {
        super.onResume()

        (activity as? VectorBaseActivity)?.supportActionBar?.setTitle(titleRes)
        Timber.v("onResume Fragment ${this.javaClass.simpleName}")
    }

    /* ==========================================================================================
     * Protected
     * ========================================================================================== */

    protected fun notImplemented() {
        // Snackbar cannot be display on PreferenceFragment
        // Snackbar.make(view!!, R.string.not_implemented, Snackbar.LENGTH_SHORT)
        activity?.toast(R.string.not_implemented)
    }

}