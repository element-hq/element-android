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

package im.vector.riotx.features.debug.sas

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import im.vector.matrix.android.api.crypto.getAllVerificationEmojis
import im.vector.riotx.R
import kotlinx.android.synthetic.main.fragment_generic_recycler_epoxy.*

class DebugSasEmojiActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fragment_generic_recycler_epoxy)

        val controller = SasEmojiController()
        epoxyRecyclerView.setController(controller)
        controller.setData(SasState(getAllVerificationEmojis()))
    }
}
