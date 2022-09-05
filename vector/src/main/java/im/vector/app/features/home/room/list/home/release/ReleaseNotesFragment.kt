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

package im.vector.app.features.home.room.list.home.release

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewpager2.widget.ViewPager2
import com.airbnb.mvrx.fragmentViewModel
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.R
import im.vector.app.core.epoxy.onClick
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.databinding.BottomSheetReleaseNotesBinding
import javax.inject.Inject

@AndroidEntryPoint
class ReleaseNotesFragment : VectorBaseFragment<BottomSheetReleaseNotesBinding>() {

    @Inject lateinit var carouselController: ReleaseNotesCarouselController
    private var tabLayoutMediator: TabLayoutMediator? = null

    private val viewModel by fragmentViewModel(ReleaseNotesViewModel::class)

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): BottomSheetReleaseNotesBinding {
        return BottomSheetReleaseNotesBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val carouselAdapter = carouselController.adapter
        views.releaseNotesCarousel.adapter = carouselAdapter

        tabLayoutMediator = TabLayoutMediator(views.releaseNotesCarouselIndicator, views.releaseNotesCarousel) { _, _ -> }
                .also { it.attach() }

        views.releaseNotesCarousel.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                viewModel.handle(ReleaseNotesAction.PageSelected(position))
                updateButtonText(position)
            }
        })

        carouselController.setData(createCarouselData())

        views.releaseNotesBtnClose.onClick { close() }
        views.releaseNotesButtonNext.onClick {
            val isLastItemSelected = with(views.releaseNotesCarouselIndicator) {
                selectedTabPosition == tabCount - 1
            }
            viewModel.handle(ReleaseNotesAction.NextPressed(isLastItemSelected))
        }

        viewModel.observeViewEvents {
            when (it) {
                is ReleaseNotesViewEvents.SelectPage -> selectPage(it.index)
                ReleaseNotesViewEvents.Close -> close()
            }
        }
    }

    private fun createCarouselData(): ReleaseCarouselData {
        return ReleaseCarouselData(
                listOf(
                        ReleaseCarouselData.Item(
                                R.string.onboarding_new_app_layout_welcome_title,
                                R.string.onboarding_new_app_layout_welcome_message,
                                R.drawable.app_layout_onboarding_welcome
                        ),
                        ReleaseCarouselData.Item(
                                R.string.onboarding_new_app_layout_spaces_title,
                                R.string.onboarding_new_app_layout_spaces_message,
                                R.drawable.app_layout_onboarding_spaces
                        ),
                        ReleaseCarouselData.Item(
                                R.string.onboarding_new_app_layout_feedback_title,
                                R.string.onboarding_new_app_layout_feedback_message,
                                R.drawable.app_layout_onboarding_feedback
                        ),
                )
        )
    }

    private fun close() {
        requireActivity().onBackPressed()
    }

    private fun selectPage(index: Int) {
        views.releaseNotesCarouselIndicator.selectTab(views.releaseNotesCarouselIndicator.getTabAt(index))
        updateButtonText(index)
    }

    private fun updateButtonText(selectedIndex: Int) {
        val isLastItem = selectedIndex == views.releaseNotesCarouselIndicator.tabCount - 1
        if (isLastItem) {
            views.releaseNotesButtonNext.setText(R.string.onboarding_new_app_layout_button_try)
        } else {
            views.releaseNotesButtonNext.setText(R.string.action_next)
        }
    }

    override fun onDestroyView() {
        tabLayoutMediator?.detach()
        tabLayoutMediator = null

        views.releaseNotesCarousel.adapter = null
        super.onDestroyView()
    }
}
