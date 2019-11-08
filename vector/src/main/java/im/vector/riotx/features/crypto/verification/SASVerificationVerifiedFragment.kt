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
package im.vector.riotx.features.crypto.verification

import android.os.Bundle
import butterknife.OnClick
import im.vector.riotx.R
import im.vector.riotx.core.platform.VectorBaseFragment
import javax.inject.Inject

class SASVerificationVerifiedFragment @Inject constructor() : VectorBaseFragment() {

    override fun getLayoutResId() = R.layout.fragment_sas_verification_verified

    private lateinit var viewModel: SasVerificationViewModel

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        viewModel = activityViewModelProvider.get(SasVerificationViewModel::class.java)
    }

    @OnClick(R.id.sas_verification_verified_done_button)
    fun onDone() {
        viewModel.finishSuccess()
    }
}
