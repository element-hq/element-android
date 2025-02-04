/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.home.room.list.home.release

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.viewpager2.widget.ViewPager2
import com.airbnb.mvrx.fragmentViewModel
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.R
import im.vector.app.core.epoxy.onClick
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.databinding.BottomSheetReleaseNotesBinding
import im.vector.lib.strings.CommonStrings
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

        val pageCallback = object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                viewModel.handle(ReleaseNotesAction.PageSelected(position))
                updateButtonText(position)
            }
        }

        viewLifecycleOwner.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onCreate(owner: LifecycleOwner) {
                views.releaseNotesCarousel.registerOnPageChangeCallback(pageCallback)
            }

            override fun onDestroy(owner: LifecycleOwner) {
                views.releaseNotesCarousel.unregisterOnPageChangeCallback(pageCallback)
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
                                CommonStrings.onboarding_new_app_layout_welcome_title,
                                CommonStrings.onboarding_new_app_layout_welcome_message,
                                R.drawable.ill_app_layout_onboarding_rooms
                        ),
                        ReleaseCarouselData.Item(
                                CommonStrings.onboarding_new_app_layout_spaces_title,
                                CommonStrings.onboarding_new_app_layout_spaces_message,
                                R.drawable.ill_app_layout_onboarding_spaces
                        ),
                        ReleaseCarouselData.Item(
                                CommonStrings.onboarding_new_app_layout_feedback_title,
                                CommonStrings.onboarding_new_app_layout_feedback_message,
                                R.drawable.ill_app_layout_onboarding_rooms
                        ),
                )
        )
    }

    private fun close() {
        requireActivity().finish()
    }

    private fun selectPage(index: Int) {
        views.releaseNotesCarouselIndicator.selectTab(views.releaseNotesCarouselIndicator.getTabAt(index))
        updateButtonText(index)
    }

    private fun updateButtonText(selectedIndex: Int) {
        val isLastItem = selectedIndex == views.releaseNotesCarouselIndicator.tabCount - 1
        if (isLastItem) {
            views.releaseNotesButtonNext.setText(CommonStrings.onboarding_new_app_layout_button_try)
        } else {
            views.releaseNotesButtonNext.setText(CommonStrings.action_next)
        }
    }

    override fun onDestroyView() {
        tabLayoutMediator?.detach()
        tabLayoutMediator = null

        views.releaseNotesCarousel.adapter = null
        super.onDestroyView()
    }
}
