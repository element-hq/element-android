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
package im.vector.reactions

import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.TypedValue
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.SearchView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.provider.FontRequest
import androidx.core.provider.FontsContractCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.google.android.material.tabs.TabLayout
import timber.log.Timber


/**
 *
 * TODO: Loading indicator while getting emoji data source?
 * TODO: migrate to maverick
 */
class EmojiReactionPickerActivity : AppCompatActivity() {

    private lateinit var tabLayout: TabLayout

    lateinit var viewModel: EmojiChooserViewModel

    private var mHandler: Handler? = null

    private var tabLayoutSelectionListener = object : TabLayout.BaseOnTabSelectedListener<TabLayout.Tab> {
        override fun onTabReselected(p0: TabLayout.Tab) {
        }

        override fun onTabUnselected(p0: TabLayout.Tab) {
        }

        override fun onTabSelected(p0: TabLayout.Tab) {
            viewModel.scrollToSection(p0.position)
        }

    }

    private fun getFontThreadHandler(): Handler {
        if (mHandler == null) {
            val handlerThread = HandlerThread("fonts")
            handlerThread.start()
            mHandler = Handler(handlerThread.looper)
        }
        return mHandler!!
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestEmojivUnicode10CompatibleFont()


        setContentView(R.layout.activity_emoji_reaction_picker)
        setSupportActionBar(findViewById(R.id.toolbar))

        tabLayout = findViewById(R.id.tabs)



        viewModel = ViewModelProviders.of(this).get(EmojiChooserViewModel::class.java)

        viewModel.emojiSourceLiveData.observe(this, Observer {
            it.rawData?.categories?.let { categories ->
                for (category in categories) {
                    val s = category.emojis[0]
                    tabLayout.addTab(tabLayout.newTab().setText(it.rawData!!.emojis[s]!!.emojiString()))
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
        supportActionBar?.title = getString(R.string.title_activity_emoji_reaction_picker)
    }

    private fun requestEmojivUnicode10CompatibleFont() {
        val fontRequest = FontRequest(
                "com.google.android.gms.fonts",
                "com.google.android.gms",
                "Noto Color Emoji Compat",
                R.array.com_google_android_gms_fonts_certs
        )

        EmojiDrawView.configureTextPaint(this, null)
        val callback = object : FontsContractCompat.FontRequestCallback() {

            override fun onTypefaceRetrieved(typeface: Typeface) {
                EmojiDrawView.configureTextPaint(this@EmojiReactionPickerActivity, typeface)
            }

            override fun onTypefaceRequestFailed(reason: Int) {
                Timber.e("Failed to load Emoji Compatible font, reason:$reason")
            }
        }

        FontsContractCompat.requestFont(this, fontRequest, callback, getFontThreadHandler())
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.menu_emoji_reaction_picker, menu)

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
        fun intent(context: Context): Intent {
            val intent = Intent(context, EmojiReactionPickerActivity::class.java)
//            intent.putExtra(EXTRA_MATRIX_ID, matrixID)
            return intent
        }
    }
}
