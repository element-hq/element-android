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

package im.vector.app.features.settings.devtools

import android.app.Activity
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import androidx.viewpager2.widget.ViewPager2.SCROLL_STATE_IDLE
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import com.google.android.material.tabs.TabLayoutMediator
import im.vector.app.R
import im.vector.app.core.extensions.exhaustive
import im.vector.app.core.extensions.registerStartForActivityResult
import im.vector.app.core.platform.VectorBaseActivity
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.core.utils.selectTxtFileToWrite
import kotlinx.android.synthetic.main.fragment_devtool_keyrequests.*
import org.matrix.android.sdk.api.extensions.tryOrNull
import javax.inject.Inject

class KeyRequestsFragment @Inject constructor(
        val viewModelFactory: KeyRequestViewModel.Factory) : VectorBaseFragment() {

    override fun getLayoutResId(): Int = R.layout.fragment_devtool_keyrequests

    override fun onResume() {
        super.onResume()
        (activity as? VectorBaseActivity)?.supportActionBar?.setTitle(R.string.key_share_request)
    }

    private var mPagerAdapter: KeyReqPagerAdapter? = null

    private val viewModel: KeyRequestViewModel by fragmentViewModel()

    override fun getMenuRes(): Int = R.menu.menu_audit

    private val pageAdapterListener = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            invalidateOptionsMenu()
        }

        override fun onPageScrollStateChanged(state: Int) {
            childFragmentManager.fragments.forEach {
                setHasOptionsMenu(state == SCROLL_STATE_IDLE)
            }
            invalidateOptionsMenu()
        }
    }

    override fun invalidate() = withState(viewModel) {
        when (it.exporting) {
            is Loading -> exportWaitingView.isVisible = true
            else       -> exportWaitingView.isVisible = false
        }
    }

    override fun onDestroy() {
        invalidateOptionsMenu()
        super.onDestroy()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mPagerAdapter = KeyReqPagerAdapter(this)
        devToolKeyRequestPager.adapter = mPagerAdapter
        devToolKeyRequestPager.registerOnPageChangeCallback(pageAdapterListener)

        TabLayoutMediator(devToolKeyRequestTabs, devToolKeyRequestPager) { tab, position ->
            when (position) {
                0 -> {
                    tab.text = "Outgoing"
                }
                1 -> {
                    tab.text = "Incoming"
                }
                2 -> {
                    tab.text = "Audit Trail"
                }
            }
        }.attach()

        viewModel.observeViewEvents {
            when (it) {
                is KeyRequestEvents.SaveAudit -> {
                    tryOrNull {
                        requireContext().contentResolver?.openOutputStream(it.uri)
                                ?.use { os -> os.write(it.raw.toByteArray()) }
                    }
                }
            }.exhaustive
        }
    }

    override fun onDestroyView() {
        devToolKeyRequestPager.unregisterOnPageChangeCallback(pageAdapterListener)
        mPagerAdapter = null
        super.onDestroyView()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.audit_export) {
            selectTxtFileToWrite(
                    activity = requireActivity(),
                    activityResultLauncher = epxortAuditForActivityResult,
                    defaultFileName = "audit-export_${System.currentTimeMillis()}.txt",
                    chooserHint = "Export Audit"
            )
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private val epxortAuditForActivityResult = registerStartForActivityResult { activityResult ->
        if (activityResult.resultCode == Activity.RESULT_OK) {
            val uri = activityResult.data?.data
            if (uri != null) {
                viewModel.handle(KeyRequestAction.ExportAudit(uri))
            }
        }
    }

    private inner class KeyReqPagerAdapter(fa: Fragment) : FragmentStateAdapter(fa) {
        override fun getItemCount(): Int = 3

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0    -> {
                    childFragmentManager.fragmentFactory.instantiate(requireContext().classLoader, OutgoingKeyRequestListFragment::class.java.name)
                }
                1    -> {
                    childFragmentManager.fragmentFactory.instantiate(requireContext().classLoader, IncomingKeyRequestListFragment::class.java.name)
                }
                else -> {
                    childFragmentManager.fragmentFactory.instantiate(requireContext().classLoader, GossipingEventsPaperTrailFragment::class.java.name)
                }
            }
        }
    }
}
