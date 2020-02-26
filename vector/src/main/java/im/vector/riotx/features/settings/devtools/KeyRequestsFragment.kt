/*
 * Copyright (c) 2020 New Vector Ltd
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

package im.vector.riotx.features.settings.devtools

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import im.vector.riotx.R
import im.vector.riotx.core.platform.VectorBaseActivity
import im.vector.riotx.core.platform.VectorBaseFragment
import kotlinx.android.synthetic.main.fragment_devtool_keyrequests.*
import javax.inject.Inject

class KeyRequestsFragment @Inject constructor() : VectorBaseFragment() {

    override fun getLayoutResId(): Int = R.layout.fragment_devtool_keyrequests

    override fun onResume() {
        super.onResume()
        (activity as? VectorBaseActivity)?.supportActionBar?.setTitle(R.string.key_share_request)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        devToolKeyRequestPager.adapter = KeyReqPagerAdapter(requireActivity())

        TabLayoutMediator(devToolKeyRequestTabs, devToolKeyRequestPager) { tab, _ ->
            tab.text = "Outgoing"
        }.attach()

    }

    private inner class KeyReqPagerAdapter(fa: FragmentActivity) : FragmentStateAdapter(fa) {
        override fun getItemCount(): Int = 1

        override fun createFragment(position: Int): Fragment {
            return childFragmentManager.fragmentFactory.instantiate(requireContext().classLoader, KeyRequestListFragment::class.java.name)
        }
    }
}
