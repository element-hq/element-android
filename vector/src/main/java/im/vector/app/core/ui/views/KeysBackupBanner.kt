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

package im.vector.app.core.ui.views

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.edit
import androidx.core.view.isVisible
import androidx.preference.PreferenceManager
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnClick
import im.vector.app.R
import timber.log.Timber

/**
 * The view used in VectorHomeActivity to show some information about the keys backup state
 * It does have a unique render method
 */
class KeysBackupBanner @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr), View.OnClickListener {

    @BindView(R.id.view_keys_backup_banner_text_1)
    lateinit var textView1: TextView

    @BindView(R.id.view_keys_backup_banner_text_2)
    lateinit var textView2: TextView

    @BindView(R.id.view_keys_backup_banner_close_group)
    lateinit var close: View

    @BindView(R.id.view_keys_backup_banner_loading)
    lateinit var loading: View

    var delegate: Delegate? = null
    private var state: State = State.Initial

    init {
        setupView()
        PreferenceManager.getDefaultSharedPreferences(context).edit {
            putBoolean(BANNER_SETUP_DO_NOT_SHOW_AGAIN, false)
            putString(BANNER_RECOVER_DO_NOT_SHOW_FOR_VERSION, "")
        }
    }

    /**
     * This methods is responsible for rendering the view according to the newState
     *
     * @param newState the newState representing the view
     */
    fun render(newState: State, force: Boolean = false) {
        if (newState == state && !force) {
            Timber.v("State unchanged")
            return
        }
        Timber.v("Rendering $newState")

        state = newState

        hideAll()
        when (newState) {
            State.Initial    -> renderInitial()
            State.Hidden     -> renderHidden()
            is State.Setup   -> renderSetup(newState.numberOfKeys)
            is State.Recover -> renderRecover(newState.version)
            is State.Update  -> renderUpdate(newState.version)
            State.BackingUp  -> renderBackingUp()
        }
    }

    override fun onClick(v: View?) {
        when (state) {
            is State.Setup   -> {
                delegate?.setupKeysBackup()
            }
            is State.Update,
            is State.Recover -> {
                delegate?.recoverKeysBackup()
            }
        }
    }

    @OnClick(R.id.view_keys_backup_banner_close)
    internal fun onCloseClicked() {
        state.let {
            when (it) {
                is State.Setup   -> {
                    PreferenceManager.getDefaultSharedPreferences(context).edit {
                        putBoolean(BANNER_SETUP_DO_NOT_SHOW_AGAIN, true)
                    }
                }
                is State.Recover -> {
                    PreferenceManager.getDefaultSharedPreferences(context).edit {
                        putString(BANNER_RECOVER_DO_NOT_SHOW_FOR_VERSION, it.version)
                    }
                }
                is State.Update  -> {
                    PreferenceManager.getDefaultSharedPreferences(context).edit {
                        putString(BANNER_UPDATE_DO_NOT_SHOW_FOR_VERSION, it.version)
                    }
                }
                else             -> {
                    // Should not happen, close button is not displayed in other cases
                }
            }
        }

        // Force refresh
        render(state, true)
    }

    // PRIVATE METHODS ****************************************************************************************************************************************

    private fun setupView() {
        inflate(context, R.layout.view_keys_backup_banner, this)
        ButterKnife.bind(this)

        setOnClickListener(this)
        textView1.setOnClickListener(this)
        textView2.setOnClickListener(this)
    }

    private fun renderInitial() {
        isVisible = false
    }

    private fun renderHidden() {
        isVisible = false
    }

    private fun renderSetup(nbOfKeys: Int) {
        if (nbOfKeys == 0
                || PreferenceManager.getDefaultSharedPreferences(context).getBoolean(BANNER_SETUP_DO_NOT_SHOW_AGAIN, false)) {
            // Do not display the setup banner if there is no keys to backup, or if the user has already closed it
            isVisible = false
        } else {
            isVisible = true

            textView1.setText(R.string.secure_backup_banner_setup_line1)
            textView2.isVisible = true
            textView2.setText(R.string.secure_backup_banner_setup_line2)
            close.isVisible = true
        }
    }

    private fun renderRecover(version: String) {
        if (version == PreferenceManager.getDefaultSharedPreferences(context).getString(BANNER_RECOVER_DO_NOT_SHOW_FOR_VERSION, null)) {
            isVisible = false
        } else {
            isVisible = true

            textView1.setText(R.string.keys_backup_banner_recover_line1)
            textView2.isVisible = true
            textView2.setText(R.string.keys_backup_banner_recover_line2)
            close.isVisible = true
        }
    }

    private fun renderUpdate(version: String) {
        if (version == PreferenceManager.getDefaultSharedPreferences(context).getString(BANNER_UPDATE_DO_NOT_SHOW_FOR_VERSION, null)) {
            isVisible = false
        } else {
            isVisible = true

            textView1.setText(R.string.keys_backup_banner_update_line1)
            textView2.isVisible = true
            textView2.setText(R.string.keys_backup_banner_update_line2)
            close.isVisible = true
        }
    }

    private fun renderBackingUp() {
        isVisible = true
        textView1.setText(R.string.secure_backup_banner_setup_line1)
        textView2.isVisible = true
        textView2.setText(R.string.keys_backup_banner_in_progress)
        loading.isVisible = true
    }

    /**
     * Hide all views that are not visible in all state
     */
    private fun hideAll() {
        textView2.isVisible = false
        close.isVisible = false
        loading.isVisible = false
    }

    /**
     * The state representing the view
     * It can take one state at a time
     */
    sealed class State {
        // Not yet rendered
        object Initial : State()

        // View will be Gone
        object Hidden : State()

        // Keys backup is not setup, numberOfKeys is the number of locally stored keys
        data class Setup(val numberOfKeys: Int) : State()

        // Keys backup can be recovered, with version from the server
        data class Recover(val version: String) : State()

        // Keys backup can be updated
        data class Update(val version: String) : State()

        // Keys are backing up
        object BackingUp : State()
    }

    /**
     * An interface to delegate some actions to another object
     */
    interface Delegate {
        fun setupKeysBackup()
        fun recoverKeysBackup()
    }

    companion object {
        /**
         * Preference key for setup. Value is a boolean.
         */
        private const val BANNER_SETUP_DO_NOT_SHOW_AGAIN = "BANNER_SETUP_DO_NOT_SHOW_AGAIN"

        /**
         * Preference key for recover. Value is a backup version (String).
         */
        private const val BANNER_RECOVER_DO_NOT_SHOW_FOR_VERSION = "BANNER_RECOVER_DO_NOT_SHOW_FOR_VERSION"

        /**
         * Preference key for update. Value is a backup version (String).
         */
        private const val BANNER_UPDATE_DO_NOT_SHOW_FOR_VERSION = "BANNER_UPDATE_DO_NOT_SHOW_FOR_VERSION"

        /**
         * Inform the banner that a Recover has been done for this version, so do not show the Recover banner for this version
         */
        fun onRecoverDoneForVersion(context: Context, version: String) {
            PreferenceManager.getDefaultSharedPreferences(context).edit {
                putString(BANNER_RECOVER_DO_NOT_SHOW_FOR_VERSION, version)
            }
        }
    }
}
