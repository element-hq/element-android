/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.call.dialpad

import android.os.Bundle
import androidx.appcompat.app.AppCompatDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.core.extensions.addFragment
import im.vector.app.core.platform.SimpleFragmentActivity
import im.vector.app.features.call.webrtc.WebRtcCallManager
import im.vector.app.features.createdirect.DirectRoomHelper
import im.vector.lib.strings.CommonStrings
import im.vector.lib.ui.styles.dialogs.MaterialProgressDialog
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.session.Session
import javax.inject.Inject

@AndroidEntryPoint
class PstnDialActivity : SimpleFragmentActivity() {

    @Inject lateinit var callManager: WebRtcCallManager
    @Inject lateinit var directRoomHelper: DirectRoomHelper
    @Inject lateinit var session: Session

    private var progress: AppCompatDialog? = null

    override fun getTitleRes(): Int = CommonStrings.call

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
                putString(DialPadFragment.EXTRA_REGION_CODE, vectorLocale.applicationLocale.country)
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
                .show(getString(CommonStrings.please_wait))
    }

    private fun dismissLoadingDialog() {
        progress?.dismiss()
    }

    private fun displayErrorDialog(throwable: Throwable) {
        MaterialAlertDialogBuilder(this)
                .setTitle(CommonStrings.dialog_title_error)
                .setMessage(errorFormatter.toHumanReadable(throwable))
                .setPositiveButton(CommonStrings.ok, null)
                .show()
    }
}
