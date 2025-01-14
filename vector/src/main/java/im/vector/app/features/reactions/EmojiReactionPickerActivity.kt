/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package im.vector.app.features.reactions

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.util.TypedValue
import android.view.Menu
import android.view.MenuItem
import android.widget.SearchView
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.airbnb.mvrx.viewModel
import com.google.android.material.tabs.TabLayout
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.EmojiCompatFontProvider
import im.vector.app.R
import im.vector.app.core.extensions.observeEvent
import im.vector.app.core.platform.VectorBaseActivity
import im.vector.app.core.platform.VectorMenuProvider
import im.vector.app.databinding.ActivityEmojiReactionPickerBinding
import im.vector.app.features.reactions.data.EmojiDataSource
import im.vector.lib.core.utils.flow.throttleFirst
import im.vector.lib.strings.CommonStrings
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import reactivecircus.flowbinding.android.widget.queryTextChanges
import javax.inject.Inject

/**
 *
 * TODO Loading indicator while getting emoji data source?
 * TODO Finish Refactor to vector base activity
 */
@AndroidEntryPoint
class EmojiReactionPickerActivity :
        VectorBaseActivity<ActivityEmojiReactionPickerBinding>(),
        EmojiCompatFontProvider.FontProviderListener,
        VectorMenuProvider {

    lateinit var viewModel: EmojiChooserViewModel

    override fun getMenuRes() = R.menu.menu_emoji_reaction_picker

    override fun handleMenuItemSelected(item: MenuItem) = false

    override fun getBinding() = ActivityEmojiReactionPickerBinding.inflate(layoutInflater)

    override fun getCoordinatorLayout() = views.coordinatorLayout

    override fun getTitleRes() = CommonStrings.title_activity_emoji_reaction_picker

    @Inject lateinit var emojiCompatFontProvider: EmojiCompatFontProvider
    @Inject lateinit var emojiDataSource: EmojiDataSource

    private val searchResultViewModel: EmojiSearchResultViewModel by viewModel()

    private var tabLayoutSelectionListener = object : TabLayout.OnTabSelectedListener {
        override fun onTabReselected(tab: TabLayout.Tab) {
        }

        override fun onTabUnselected(tab: TabLayout.Tab) {
        }

        override fun onTabSelected(tab: TabLayout.Tab) {
            viewModel.scrollToSection(tab.position)
        }
    }

    override fun initUiAndData() {
        setupToolbar(views.emojiPickerToolbar)
                .allowBack()
        emojiCompatFontProvider.let {
            EmojiDrawView.configureTextPaint(this, it.typeface)
            it.addListener(this)
        }

        viewModel = viewModelProvider.get(EmojiChooserViewModel::class.java)

        viewModel.eventId = intent.getStringExtra(EXTRA_EVENT_ID)
        lifecycleScope.launch {
            val rawData = emojiDataSource.rawData.await()
            rawData.categories.forEach { category ->
                val s = category.emojis[0]
                views.tabs.newTab()
                        .also { tab ->
                            tab.text = rawData.emojis[s]!!.emoji
                            tab.contentDescription = category.name
                        }
                        .also { tab ->
                            views.tabs.addTab(tab)
                        }
            }
        }
        views.tabs.addOnTabSelectedListener(tabLayoutSelectionListener)

        viewModel.currentSection.observe(this) { section ->
            section?.let {
                views.tabs.removeOnTabSelectedListener(tabLayoutSelectionListener)
                views.tabs.getTabAt(it)?.select()
                views.tabs.addOnTabSelectedListener(tabLayoutSelectionListener)
            }
        }

        viewModel.navigateEvent.observeEvent(this) {
            if (it == EmojiChooserViewModel.NAVIGATE_FINISH) {
                // finish with result
                val dataResult = Intent()
                dataResult.putExtra(EXTRA_REACTION_RESULT, viewModel.selectedReaction)
                dataResult.putExtra(EXTRA_EVENT_ID, viewModel.eventId)
                setResult(Activity.RESULT_OK, dataResult)
                finish()
            }
        }

        views.emojiPickerWholeListFragmentContainer.isVisible = true
        views.emojiPickerFilteredListFragmentContainer.isVisible = false
        views.tabs.isVisible = true
    }

    override fun compatibilityFontUpdate(typeface: Typeface?) {
        EmojiDrawView.configureTextPaint(this, typeface)
    }

    override fun onDestroy() {
        emojiCompatFontProvider.removeListener(this)
        super.onDestroy()
    }

    override fun handlePostCreateMenu(menu: Menu) {
        val searchItem = menu.findItem(R.id.search)
        (searchItem.actionView as? SearchView)?.let { searchView ->
            searchItem.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
                override fun onMenuItemActionExpand(p0: MenuItem): Boolean {
                    searchView.isIconified = false
                    searchView.requestFocusFromTouch()
                    // we want to force the tool bar as visible even if hidden with scroll flags
                    views.emojiPickerToolbar.minimumHeight = getActionBarSize()
                    return true
                }

                override fun onMenuItemActionCollapse(p0: MenuItem): Boolean {
                    // when back, clear all search
                    views.emojiPickerToolbar.minimumHeight = 0
                    searchView.setQuery("", true)
                    return true
                }
            })

            searchView.setOnCloseListener {
                currentFocus?.clearFocus()
                searchItem.collapseActionView()
                true
            }

            searchView.queryTextChanges()
                    .throttleFirst(600)
                    .onEach { query ->
                        onQueryText(query.toString())
                    }
                    .launchIn(lifecycleScope)
        }
        searchItem.expandActionView()
    }

    // TODO move to ThemeUtils when core module is created
    private fun getActionBarSize(): Int {
        return try {
            val typedValue = TypedValue()
            theme.resolveAttribute(com.google.android.material.R.attr.actionBarSize, typedValue, true)
            TypedValue.complexToDimensionPixelSize(typedValue.data, resources.displayMetrics)
        } catch (e: Exception) {
            // Timber.e(e, "Unable to get color")
            TypedValue.complexToDimensionPixelSize(56, resources.displayMetrics)
        }
    }

    private fun onQueryText(query: String) {
        if (query.isEmpty()) {
            views.tabs.isVisible = true
            views.emojiPickerWholeListFragmentContainer.isVisible = true
            views.emojiPickerFilteredListFragmentContainer.isVisible = false
        } else {
            views.tabs.isVisible = false
            views.emojiPickerWholeListFragmentContainer.isVisible = false
            views.emojiPickerFilteredListFragmentContainer.isVisible = true
            searchResultViewModel.handle(EmojiSearchAction.UpdateQuery(query))
        }
    }

    companion object {

        private const val EXTRA_EVENT_ID = "EXTRA_EVENT_ID"
        private const val EXTRA_REACTION_RESULT = "EXTRA_REACTION_RESULT"

        fun intent(context: Context, eventId: String): Intent {
            val intent = Intent(context, EmojiReactionPickerActivity::class.java)
            intent.putExtra(EXTRA_EVENT_ID, eventId)
            return intent
        }

        fun getOutputEventId(data: Intent?): String? = data?.getStringExtra(EXTRA_EVENT_ID)

        fun getOutputReaction(data: Intent?): String? = data?.getStringExtra(EXTRA_REACTION_RESULT)
    }
}
