/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.core.ui.views

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import im.vector.app.R
import im.vector.app.databinding.ViewKeysBackupBannerBinding
import im.vector.app.features.workers.signout.BannerState
import im.vector.lib.strings.CommonStrings
import timber.log.Timber

/**
 * The view used in VectorHomeActivity to show some information about the keys backup state.
 * It does have a unique render method.
 */
class KeysBackupBanner @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr), View.OnClickListener {

    var delegate: Delegate? = null
    private var state: BannerState = BannerState.Initial

    private lateinit var views: ViewKeysBackupBannerBinding

    init {
        setupView()
    }

    /**
     * This methods is responsible for rendering the view according to the newState.
     *
     * @param newState the newState representing the view
     * @param force true to force the rendering of the view
     */
    fun render(newState: BannerState, force: Boolean = false) {
        if (newState == state && !force) {
            Timber.v("State unchanged")
            return
        }
        Timber.v("Rendering $newState")

        state = newState

        hideAll()
        when (newState) {
            BannerState.Initial -> renderInitial()
            BannerState.Hidden -> renderHidden()
            is BannerState.Setup -> renderSetup(newState)
            is BannerState.Recover -> renderRecover(newState)
            is BannerState.Update -> renderUpdate(newState)
            BannerState.BackingUp -> renderBackingUp()
        }
    }

    override fun onClick(v: View?) {
        when (state) {
            is BannerState.Setup -> delegate?.setupKeysBackup()
            is BannerState.Update,
            is BannerState.Recover -> delegate?.recoverKeysBackup()
            else -> Unit
        }
    }

    private fun onCloseClicked() {
        delegate?.onCloseClicked()
        // Force refresh
        render(state, true)
    }

    // PRIVATE METHODS ****************************************************************************************************************************************

    private fun setupView() {
        inflate(context, R.layout.view_keys_backup_banner, this)

        setOnClickListener(this)
        views = ViewKeysBackupBannerBinding.bind(this)
        views.viewKeysBackupBannerText1.setOnClickListener(this)
        views.viewKeysBackupBannerText2.setOnClickListener(this)
        views.viewKeysBackupBannerClose.setOnClickListener { onCloseClicked() }
    }

    private fun renderInitial() {
        isVisible = false
    }

    private fun renderHidden() {
        isVisible = false
    }

    private fun renderSetup(state: BannerState.Setup) {
        if (state.numberOfKeys == 0 || state.doNotShowAgain) {
            // Do not display the setup banner if there is no keys to backup, or if the user has already closed it
            isVisible = false
        } else {
            isVisible = true

            views.viewKeysBackupBannerText1.setText(CommonStrings.secure_backup_banner_setup_line1)
            views.viewKeysBackupBannerText2.isVisible = true
            views.viewKeysBackupBannerText2.setText(CommonStrings.secure_backup_banner_setup_line2)
            views.viewKeysBackupBannerCloseGroup.isVisible = true
        }
    }

    private fun renderRecover(state: BannerState.Recover) {
        if (state.version == state.doNotShowForVersion) {
            isVisible = false
        } else {
            isVisible = true

            views.viewKeysBackupBannerText1.setText(CommonStrings.keys_backup_banner_recover_line1)
            views.viewKeysBackupBannerText2.isVisible = true
            views.viewKeysBackupBannerText2.setText(CommonStrings.keys_backup_banner_recover_line2)
            views.viewKeysBackupBannerCloseGroup.isVisible = true
        }
    }

    private fun renderUpdate(state: BannerState.Update) {
        if (state.version == state.doNotShowForVersion) {
            isVisible = false
        } else {
            isVisible = true

            views.viewKeysBackupBannerText1.setText(CommonStrings.keys_backup_banner_update_line1)
            views.viewKeysBackupBannerText2.isVisible = true
            views.viewKeysBackupBannerText2.setText(CommonStrings.keys_backup_banner_update_line2)
            views.viewKeysBackupBannerCloseGroup.isVisible = true
        }
    }

    private fun renderBackingUp() {
        isVisible = true
        views.viewKeysBackupBannerText1.setText(CommonStrings.secure_backup_banner_setup_line1)
        views.viewKeysBackupBannerText2.isVisible = true
        views.viewKeysBackupBannerText2.setText(CommonStrings.keys_backup_banner_in_progress)
        views.viewKeysBackupBannerLoading.isVisible = true
    }

    /**
     * Hide all views that are not visible in all state.
     */
    private fun hideAll() {
        views.viewKeysBackupBannerText2.isVisible = false
        views.viewKeysBackupBannerCloseGroup.isVisible = false
        views.viewKeysBackupBannerLoading.isVisible = false
    }

    /**
     * An interface to delegate some actions to another object.
     */
    interface Delegate {
        fun onCloseClicked()
        fun setupKeysBackup()
        fun recoverKeysBackup()
    }
}
