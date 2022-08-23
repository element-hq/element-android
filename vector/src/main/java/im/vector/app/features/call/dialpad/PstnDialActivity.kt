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

package im.vector.app.features.call.dialpad

import android.os.Bundle
import androidx.appcompat.app.AppCompatDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.R
import im.vector.app.core.error.ErrorFormatter
import im.vector.app.core.extensions.addFragment
import im.vector.app.core.platform.SimpleFragmentActivity
import im.vector.app.features.call.webrtc.WebRtcCallManager
import im.vector.app.features.createdirect.DirectRoomHelper
import im.vector.app.features.settings.VectorLocale
import im.vector.lib.ui.styles.dialogs.MaterialProgressDialog
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.session.Session
import javax.inject.Inject

@AndroidEntryPoint
class PstnDialActivity : SimpleFragmentActivity() {

    @Inject lateinit var callManager: WebRtcCallManager
    @Inject lateinit var directRoomHelper: DirectRoomHelper
    @Inject lateinit var session: Session
    @Inject lateinit var errorFormatter: ErrorFormatter

    private var progress: AppCompatDialog? = null

    override fun getTitleRes(): Int = R.string.call

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (isFirstCreation()) {
            addFragment(
                    views.container,
                    createDialPadFragment()
            )
        }
    }

    private fun handleStartCallWithPhoneNumber(rawNumber: String) {
        lifecycleScope.launch {
            try {
                showLoadingDialog()
                val result = DialPadLookup(session, callManager, directRoomHelper).lookupPhoneNumber(rawNumber)
                callManager.startOutgoingCall(result.roomId, result.userId, isVideoCall = false)
                dismissLoadingDialog()
                finish()
            } catch (failure: Throwable) {
                dismissLoadingDialog()
                displayErrorDialog(failure)
            }
        }
    }

    private fun createDialPadFragment(): Fragment {
        val fragment = supportFragmentManager.fragmentFactory.instantiate(classLoader, DialPadFragment::class.java.name)
        return (fragment as DialPadFragment).apply {
            arguments = Bundle().apply {
                putBoolean(DialPadFragment.EXTRA_ENABLE_DELETE, true)
                putBoolean(DialPadFragment.EXTRA_ENABLE_OK, true)
                putString(DialPadFragment.EXTRA_REGION_CODE, VectorLocale.applicationLocale.country)
            }
            callback = object : DialPadFragment.Callback {
                override fun onOkClicked(formatted: String?, raw: String?) {
                    if (raw.isNullOrEmpty()) return
                    handleStartCallWithPhoneNumber(raw)
                }
            }
        }
    }

    private fun showLoadingDialog() {
        progress?.dismiss()
        progress = MaterialProgressDialog(this)
                .show(getString(R.string.please_wait))
    }

    private fun dismissLoadingDialog() {
        progress?.dismiss()
    }

    private fun displayErrorDialog(throwable: Throwable) {
        MaterialAlertDialogBuilder(this)
                .setTitle(R.string.dialog_title_error)
                .setMessage(errorFormatter.toHumanReadable(throwable))
                .setPositiveButton(R.string.ok, null)
                .show()
    }
}
