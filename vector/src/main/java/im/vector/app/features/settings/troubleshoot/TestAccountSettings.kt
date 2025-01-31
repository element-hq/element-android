/*
 * Copyright 2018-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package im.vector.app.features.settings.troubleshoot

import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import im.vector.app.R
import im.vector.app.core.di.ActiveSessionHolder
import im.vector.app.core.resources.StringProvider
import im.vector.app.features.session.coroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.matrix.android.sdk.api.extensions.tryOrNull
import org.matrix.android.sdk.api.session.pushrules.RuleIds
import org.matrix.android.sdk.api.session.pushrules.RuleKind
import javax.inject.Inject

/**
 * Check that the main pushRule (RULE_ID_DISABLE_ALL) is correctly setup.
 */
class TestAccountSettings @Inject constructor(
        private val stringProvider: StringProvider,
        private val activeSessionHolder: ActiveSessionHolder
) :
        TroubleshootTest(R.string.settings_troubleshoot_test_account_settings_title) {

    override fun perform(activityResultLauncher: ActivityResultLauncher<Intent>) {
        val session = activeSessionHolder.getSafeActiveSession() ?: return
        val defaultRule = session.pushRuleService().getPushRules().getAllRules()
                .find { it.ruleId == RuleIds.RULE_ID_DISABLE_ALL }

        if (defaultRule != null) {
            if (!defaultRule.enabled) {
                description = stringProvider.getString(R.string.settings_troubleshoot_test_account_settings_success)
                quickFix = null
                status = TestStatus.SUCCESS
            } else {
                description = stringProvider.getString(R.string.settings_troubleshoot_test_account_settings_failed)
                quickFix = object : TroubleshootQuickFix(R.string.settings_troubleshoot_test_account_settings_quickfix) {
                    override fun doFix() {
                        if (manager?.diagStatus == TestStatus.RUNNING) return // wait before all is finished

                        session.coroutineScope.launch {
                            tryOrNull {
                                session.pushRuleService().updatePushRuleEnableStatus(RuleKind.OVERRIDE, defaultRule, !defaultRule.enabled)
                            }
                            withContext(Dispatchers.Main) {
                                manager?.retry(activityResultLauncher)
                            }
                        }
                    }
                }
                status = TestStatus.FAILED
            }
        } else {
            // should not happen?
            status = TestStatus.FAILED
        }
    }
}
