/*
 * Copyright 2019 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package im.vector.app.features.reactions

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.util.TypedValue
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.SearchView
import androidx.core.view.isVisible
import com.airbnb.mvrx.viewModel
import com.google.android.material.tabs.TabLayout
import com.jakewharton.rxbinding3.widget.queryTextChanges
import im.vector.app.EmojiCompatFontProvider
import im.vector.app.R
import im.vector.app.core.di.ScreenComponent
import im.vector.app.core.extensions.observeEvent
import im.vector.app.core.platform.VectorBaseActivity
import im.vector.app.databinding.ActivityEmojiReactionPickerBinding
import im.vector.app.features.reactions.data.EmojiDataSource
import io.reactivex.android.schedulers.AndroidSchedulers

import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 *
 * TODO: Loading indicator while getting emoji data source?
 * TODO: Finish Refactor to vector base activity
 */
class EmojiReactionPickerActivity : VectorBaseActivity<ActivityEmojiReactionPickerBinding>(),
        EmojiCompatFontProvider.FontProviderListener {

    lateinit var viewModel: EmojiChooserViewModel

    override fun getMenuRes() = R.menu.menu_emoji_reaction_picker

    override fun getBinding() = ActivityEmojiReactionPickerBinding.inflate(layoutInflater)

    override fun getCoordinatorLayout() = views.coordinatorLayout

    override fun getTitleRes() = R.string.title_activity_emoji_reaction_picker

    @Inject lateinit var emojiSearchResultViewModelFactory: EmojiSearchResultViewModel.Factory
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

    override fun injectWith(injector: ScreenComponent) {
        injector.inject(this)
    }

    override fun initUiAndData() {
        configureToolbar(views.emojiPickerToolbar)
        emojiCompatFontProvider.let {
            EmojiDrawView.configureTextPaint(this, it.typeface)
            it.addListener(this)
        }

        viewModel = viewModelProvider.get(EmojiChooserViewModel::class.java)

        viewModel.eventId = intent.getStringExtra(EXTRA_EVENT_ID)

        emojiDataSource.rawData.categories.forEach { category ->
            val s = category.emojis[0]
            views.tabs.newTab()
                    .also { tab ->
                        tab.text = emojiDataSource.rawData.emojis[s]!!.emoji
                        tab.contentDescription = category.name
                    }
                    .also { tab ->
                        views.tabs.addTab(tab)
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

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(getMenuRes(), menu)

        val searchItem = menu.findItem(R.id.search)
        (searchItem.actionView as? SearchView)?.let { searchView ->
            searchItem.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
                override fun onMenuItemActionExpand(p0: MenuItem?): Boolean {
                    searchView.isIconified = false
                    searchView.requestFocusFromTouch()
                    // we want to force the tool bar as visible even if hidden with scroll flags
                    views.emojiPickerToolbar.minimumHeight = getActionBarSize()
                    return true
                }

                override fun onMenuItemActionCollapse(p0: MenuItem?): Boolean {
                    // when back, clear all search
                    views.emojiPickerToolbar.minimumHeight = 0
                    searchView.setQuery("", true)
                    return true
                }
            })

            searchView.queryTextChanges()
                    .throttleWithTimeout(600, TimeUnit.MILLISECONDS)
                    .doOnError { err -> Timber.e(err) }
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe { query ->
                        onQueryText(query.toString())
                    }
                    .disposeOnDestroy()
        }
        return true
    }

    // TODO move to ThemeUtils when core module is created
    private fun getActionBarSize(): Int {
        return try {
            val typedValue = TypedValue()
            theme.resolveAttribute(R.attr.actionBarSize, typedValue, true)
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
