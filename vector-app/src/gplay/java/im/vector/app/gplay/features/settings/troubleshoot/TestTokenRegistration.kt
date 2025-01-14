/*
 * Copyright 2018-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package im.vector.app.gplay.features.settings.troubleshoot

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import im.vector.app.core.di.ActiveSessionHolder
import im.vector.app.core.pushers.FcmHelper
import im.vector.app.core.pushers.PushersManager
import im.vector.app.core.resources.StringProvider
import im.vector.app.features.settings.troubleshoot.TroubleshootTest
import im.vector.lib.strings.CommonStrings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.session.pushers.PusherState
import javax.inject.Inject

/**
 * Force registration of the token to HomeServer
 */
class TestTokenRegistration @Inject constructor(
        private val context: FragmentActivity,
        private val stringProvider: StringProvider,
        private val pushersManager: PushersManager,
        private val activeSessionHolder: ActiveSessionHolder,
        private val fcmHelper: FcmHelper,
) :
        TroubleshootTest(CommonStrings.settings_troubleshoot_test_token_registration_title) {

    override fun perform(testParameters: TestParameters) {
        // Check if we have a registered pusher for this token
        val fcmToken = fcmHelper.getFcmToken() ?: run {
            status = TestStatus.FAILED
            return
        }
        val session = activeSessionHolder.getSafeActiveSession() ?: run {
            status = TestStatus.FAILED
            return
        }
        val pushers = session.pushersService().getPushers().filter {
            it.pushKey == fcmToken && it.state == PusherState.REGISTERED
        }
        if (pushers.isEmpty()) {
            description = stringProvider.getString(
                    CommonStrings.settings_troubleshoot_test_token_registration_failed,
                    stringProvider.getString(CommonStrings.sas_error_unknown)
            )
            quickFix = object : TroubleshootQuickFix(CommonStrings.settings_troubleshoot_test_token_registration_quick_fix) {
                override fun doFix() {
                    context.lifecycleScope.launch(Dispatchers.IO) {
                        val workId = pushersManager.enqueueRegisterPusherWithFcmKey(fcmToken)
                        WorkManager.getInstance(context).getWorkInfoByIdLiveData(workId).observe(context, Observer { workInfo ->
                            if (workInfo != null) {
                                if (workInfo.state == WorkInfo.State.SUCCEEDED) {
                                    manager?.retry(testParameters)
                                } else if (workInfo.state == WorkInfo.State.FAILED) {
                                    manager?.retry(testParameters)
                                }
                            }
                        })
                    }
                }
            }

            status = TestStatus.FAILED
        } else {
            description = stringProvider.getString(CommonStrings.settings_troubleshoot_test_token_registration_success)
            status = TestStatus.SUCCESS
        }
    }
}
