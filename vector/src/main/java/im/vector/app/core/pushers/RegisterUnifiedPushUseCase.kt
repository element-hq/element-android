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

package im.vector.app.core.pushers

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import im.vector.app.features.VectorFeatures
import org.unifiedpush.android.connector.UnifiedPush
import javax.inject.Inject

class RegisterUnifiedPushUseCase @Inject constructor(
        @ApplicationContext private val context: Context,
        private val vectorFeatures: VectorFeatures,
) {

    sealed interface RegisterUnifiedPushResult {
        object Success : RegisterUnifiedPushResult
        object NeedToAskUserForDistributor : RegisterUnifiedPushResult
    }

    fun execute(distributor: String = ""): RegisterUnifiedPushResult {
        if (distributor.isNotEmpty()) {
            saveAndRegisterApp(distributor)
            return RegisterUnifiedPushResult.Success
        }

        if (!vectorFeatures.allowExternalUnifiedPushDistributors()) {
            saveAndRegisterApp(context.packageName)
            return RegisterUnifiedPushResult.Success
        }

        if (UnifiedPush.getDistributor(context).isNotEmpty()) {
            registerApp()
            return RegisterUnifiedPushResult.Success
        }

        val distributors = UnifiedPush.getDistributors(context)

        return if (distributors.size == 1) {
            saveAndRegisterApp(distributors.first())
            RegisterUnifiedPushResult.Success
        } else {
            RegisterUnifiedPushResult.NeedToAskUserForDistributor
        }
    }

    private fun saveAndRegisterApp(distributor: String) {
        UnifiedPush.saveDistributor(context, distributor)
        registerApp()
    }

    private fun registerApp() {
        UnifiedPush.registerApp(context)
    }
}
