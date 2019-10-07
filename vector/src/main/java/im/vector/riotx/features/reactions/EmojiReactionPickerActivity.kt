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
package im.vector.riotx.features.reactions

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.util.TypedValue
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.google.android.material.tabs.TabLayout
import im.vector.riotx.EmojiCompatFontProvider
import im.vector.riotx.R
import im.vector.riotx.core.di.ScreenComponent
import im.vector.riotx.core.extensions.observeEvent
import im.vector.riotx.core.platform.VectorBaseActivity
import kotlinx.android.synthetic.main.activity_emoji_reaction_picker.*
import javax.inject.Inject

/**
 *
 * TODO: Loading indicator while getting emoji data source?
 * TODO: migrate to maverick
 * TODO: Finish Refactor to vector base activity
 * TODO: Move font request to app
 */
class EmojiReactionPickerActivity : VectorBaseActivity(), EmojiCompatFontProvider.FontProviderListener {


    private lateinit var tabLayout: TabLayout

    lateinit var viewModel: EmojiChooserViewModel

    override fun getMenuRes(): Int = R.menu.menu_emoji_reaction_picker

    override fun getLayoutRes(): Int = R.layout.activity_emoji_reaction_picker

    override fun getTitleRes(): Int = R.string.title_activity_emoji_reaction_picker

    @Inject lateinit var emojiCompatFontProvider: EmojiCompatFontProvider

    private var tabLayoutSelectionListener = object : TabLayout.BaseOnTabSelectedListener<TabLayout.Tab> {
        override fun onTabReselected(p0: TabLayout.Tab) {
        }

        override fun onTabUnselected(p0: TabLayout.Tab) {
        }

        override fun onTabSelected(p0: TabLayout.Tab) {
            viewModel.scrollToSection(p0.position)
        }

    }

    override fun injectWith(injector: ScreenComponent) {
        injector.inject(this)
    }

    override fun initUiAndData() {
        configureToolbar(emojiPickerToolbar)
        emojiCompatFontProvider.let {
            EmojiDrawView.configureTextPaint(this, it.typeface)
            it.addListener(this)
        }

        tabLayout = findViewById(R.id.tabs)

        viewModel = ViewModelProviders.of(this, viewModelFactory).get(EmojiChooserViewModel::class.java)

        viewModel.eventId = intent.getStringExtra(EXTRA_EVENT_ID)

        viewModel.emojiSourceLiveData.observe(this, Observer {
            it.rawData?.categories?.let { categories ->
                for (category in categories) {
                    val s = category.emojis[0]
                    val newTab =tabLayout.newTab()
                    newTab.setText(it.rawData!!.emojis[s]!!.emojiString())
                    newTab.setContentDescription(category.name)
                    tabLayout.addTab(newTab)
                }
                tabLayout.addOnTabSelectedListener(tabLayoutSelectionListener)
            }

        })

        viewModel.currentSection.observe(this, Observer { section ->
            section?.let {
                tabLayout.removeOnTabSelectedListener(tabLayoutSelectionListener)
                tabLayout.getTabAt(it)?.select()
                tabLayout.addOnTabSelectedListener(tabLayoutSelectionListener)
            }
        })

        viewModel.navigateEvent.observeEvent(this) {
            if (it == EmojiChooserViewModel.NAVIGATE_FINISH) {
                //finish with result
                val dataResult = Intent()
                dataResult.putExtra(EXTRA_REACTION_RESULT, viewModel.selectedReaction)
                dataResult.putExtra(EXTRA_EVENT_ID, viewModel.eventId)
                setResult(Activity.RESULT_OK, dataResult)
                finish()
            }
        }
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
        (searchItem.actionView as? SearchView)?.let {

            searchItem.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
                override fun onMenuItemActionExpand(p0: MenuItem?): Boolean {
                    it.isIconified = false
                    it.requestFocusFromTouch()
                    //we want to force the tool bar as visible even if hidden with scroll flags
                    findViewById<Toolbar>(R.id.toolbar)?.minimumHeight = getActionBarSize()
                    return true
                }

                override fun onMenuItemActionCollapse(p0: MenuItem?): Boolean {
                    // when back, clear all search
                    findViewById<Toolbar>(R.id.toolbar)?.minimumHeight = 0
                    it.setQuery("", true)
                    return true
                }
            })
        }

        return true
    }

    //TODO move to ThemeUtils when core module is created
    private fun getActionBarSize(): Int {
        return try {
            val typedValue = TypedValue()
            theme.resolveAttribute(R.attr.actionBarSize, typedValue, true)
            TypedValue.complexToDimensionPixelSize(typedValue.data, resources.displayMetrics)
        } catch (e: Exception) {
            //Timber.e(e, "Unable to get color")
            TypedValue.complexToDimensionPixelSize(56, resources.displayMetrics)
        }
    }

    companion object {

        const val EXTRA_EVENT_ID = "EXTRA_EVENT_ID"
        const val EXTRA_REACTION_RESULT = "EXTRA_REACTION_RESULT"

        fun intent(context: Context, eventId: String): Intent {
            val intent = Intent(context, EmojiReactionPickerActivity::class.java)
            intent.putExtra(EXTRA_EVENT_ID, eventId)
            return intent
        }
    }
}
