/*
 *
 *  * Copyright 2019 New Vector Ltd
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package im.vector.riotx.features.home.createdirect

import android.content.Context
import android.content.Intent
import im.vector.riotx.R
import im.vector.riotx.core.extensions.addFragment
import im.vector.riotx.core.platform.VectorBaseActivity

class CreateDirectRoomActivity : VectorBaseActivity() {

    override fun getLayoutRes() = R.layout.activity_simple

    override fun initUiAndData() {
        if (isFirstCreation()) {
            addFragment(CreateDirectRoomFragment(), R.id.simpleFragmentContainer)
        }
    }

    companion object {
        fun getIntent(context: Context): Intent {
            return Intent(context, CreateDirectRoomActivity::class.java)
        }
    }


}