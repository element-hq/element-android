/*
 * Copyright 2020 New Vector Ltd
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

package im.vector.app.features.home.room.detail.search

import android.os.Bundle
import android.os.Parcelable
import android.view.View
import com.airbnb.mvrx.args
import com.airbnb.mvrx.fragmentViewModel
import im.vector.app.R
import im.vector.app.core.platform.VectorBaseFragment
import kotlinx.android.parcel.Parcelize
import timber.log.Timber
import javax.inject.Inject

@Parcelize
data class SearchArgs(
        val roomId: String
) : Parcelable

class SearchFragment @Inject constructor(
        val viewModelFactory: SearchViewModel.Factory
) : VectorBaseFragment() {

    private val fragmentArgs: SearchArgs by args()
    private val searchViewModel: SearchViewModel by fragmentViewModel()

    override fun getLayoutResId() = R.layout.fragment_search

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

    fun search(query: String) {
        Timber.d(query)
    }
}
