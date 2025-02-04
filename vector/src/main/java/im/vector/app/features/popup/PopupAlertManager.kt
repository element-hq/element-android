/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package im.vector.app.features.popup

import android.app.Activity
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
import androidx.core.view.ViewCompat
import com.tapadoo.alerter.Alerter
import im.vector.app.R
import im.vector.app.core.extensions.giveAccessibilityFocus
import im.vector.app.core.platform.VectorBaseActivity
import im.vector.app.core.resources.StringProvider
import im.vector.app.core.utils.isAnimationEnabled
import im.vector.app.features.MainActivity
import im.vector.app.features.analytics.ui.consent.AnalyticsOptInActivity
import im.vector.app.features.home.room.list.home.release.ReleaseNotesActivity
import im.vector.app.features.pin.PinActivity
import im.vector.app.features.signout.hard.SignedOutActivity
import im.vector.app.features.themes.ThemeUtils
import im.vector.lib.core.utils.timer.Clock
import im.vector.lib.strings.CommonStrings
import timber.log.Timber
import java.lang.ref.WeakReference
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Responsible of displaying important popup alerts on top of the screen.
 * Alerts are stacked and will be displayed sequentially but sorted by priority.
 * So if a new alert is posted with a higher priority than the current one it will show it instead and the current one
 * will be back in the queue in first position.
 */
@Singleton
class PopupAlertManager @Inject constructor(
        private val clock: Clock,
        private val stringProvider: StringProvider,
) {

    companion object {
        const val INCOMING_CALL_PRIORITY = Int.MAX_VALUE
        const val INCOMING_VERIFICATION_REQUEST_PRIORITY = 1
        const val DEFAULT_PRIORITY = 0
        const val REVIEW_LOGIN_UID = "review_login"
        const val UPGRADE_SECURITY_UID = "upgrade_security"
        const val VERIFY_SESSION_UID = "verify_session"
        const val ENABLE_PUSH_UID = "enable_push"
    }

    private var weakCurrentActivity: WeakReference<Activity>? = null
    private var currentAlerter: VectorAlert? = null

    private val alertQueue = mutableListOf<VectorAlert>()

    fun hasAlertsToShow(): Boolean {
        return currentAlerter != null || alertQueue.isNotEmpty()
    }

    fun postVectorAlert(alert: VectorAlert) {
        synchronized(alertQueue) {
            alertQueue.add(alert)
        }
        weakCurrentActivity?.get()?.runOnUiThread {
            displayNextIfPossible()
        }
    }

    fun cancelAlert(uid: String) {
        synchronized(alertQueue) {
            alertQueue.listIterator().apply {
                while (this.hasNext()) {
                    val next = this.next()
                    if (next.uid == uid) {
                        this.remove()
                    }
                }
            }
        }

        // it could also be the current one
        if (currentAlerter?.uid == uid) {
            weakCurrentActivity?.get()?.runOnUiThread {
                Alerter.hide()
                currentIsDismissed()
            }
        }
    }

    /**
     * Cancel all alerts, after a sign out for instance.
     */
    fun cancelAll() {
        synchronized(alertQueue) {
            alertQueue.clear()
        }

        // Cancel any displayed alert
        weakCurrentActivity?.get()?.runOnUiThread {
            Alerter.hide()
            currentIsDismissed()
        }
    }

    fun onNewActivityDisplayed(activity: Activity) {
        // we want to remove existing popup on previous activity and display it on new one
        if (currentAlerter != null) {
            weakCurrentActivity?.get()?.let {
                Alerter.clearCurrent(it, null, null)
                if (currentAlerter?.isLight == false) {
                    setLightStatusBar()
                }
            }
        }
        weakCurrentActivity = WeakReference(activity)
        if (!shouldBeDisplayedIn(currentAlerter, activity)) {
            return
        }
        if (currentAlerter != null) {
            if (currentAlerter!!.expirationTimestamp != null && clock.epochMillis() > currentAlerter!!.expirationTimestamp!!) {
                // this alert has expired, remove it
                // perform dismiss
                try {
                    currentAlerter?.dismissedAction?.run()
                } catch (e: Exception) {
                    Timber.e("## failed to perform action")
                }
                currentAlerter = null
                Handler(Looper.getMainLooper()).postDelayed({
                    displayNextIfPossible()
                }, 2000)
            } else {
                showAlert(currentAlerter!!, activity, animate = false)
            }
        } else {
            Handler(Looper.getMainLooper()).postDelayed({
                displayNextIfPossible()
            }, 2000)
        }
    }

    private fun displayNextIfPossible() {
        val currentActivity = weakCurrentActivity?.get()
        if (currentActivity == null || currentActivity.isDestroyed) {
            // will retry later
            return
        }
        val next: VectorAlert?
        synchronized(alertQueue) {
            next = alertQueue.maxByOrNull { it.priority }
            // If next alert with highest priority is higher than the current one, we should display it
            // and add the current one to queue again.
            if (next != null && next.priority > (currentAlerter?.priority ?: Int.MIN_VALUE)) {
                alertQueue.remove(next)
                currentAlerter?.also {
                    alertQueue.add(0, it)
                }
            } else {
                // otherwise, we don't do anything
                return
            }
        }
        currentAlerter = next
        next?.let {
            if (!shouldBeDisplayedIn(next, currentActivity)) return
            val currentTime = clock.epochMillis()
            if (next.expirationTimestamp != null && currentTime > next.expirationTimestamp!!) {
                // skip
                try {
                    next.dismissedAction?.run()
                } catch (e: java.lang.Exception) {
                    Timber.e("## failed to perform action")
                }
                displayNextIfPossible()
            } else {
                showAlert(it, currentActivity)
            }
        }
    }

    private fun clearLightStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            weakCurrentActivity?.get()
                    // Do not change anything on Dark themes
                    ?.takeIf { ThemeUtils.isLightTheme(it) }
                    ?.window?.decorView
                    ?.let { view ->
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            view.windowInsetsController?.setSystemBarsAppearance(0, APPEARANCE_LIGHT_STATUS_BARS)
                        } else {
                            @Suppress("DEPRECATION")
                            view.systemUiVisibility = view.systemUiVisibility and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
                        }
                    }
        }
    }

    private fun setLightStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            weakCurrentActivity?.get()
                    // Do not change anything on Dark themes
                    ?.takeIf { ThemeUtils.isLightTheme(it) }
                    ?.window?.decorView
                    ?.let { view ->
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            view.windowInsetsController?.setSystemBarsAppearance(APPEARANCE_LIGHT_STATUS_BARS, APPEARANCE_LIGHT_STATUS_BARS)
                        } else {
                            @Suppress("DEPRECATION")
                            view.systemUiVisibility = view.systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                        }
                    }
        }
    }

    private fun showAlert(alert: VectorAlert, activity: Activity, animate: Boolean = true) {
        if (!alert.isLight) {
            clearLightStatusBar()
        }
        val noAnimation = !(animate && activity.isAnimationEnabled())

        alert.weakCurrentActivity = WeakReference(activity)
        val alerter = Alerter.create(activity, alert.layoutRes)

        alerter.setTitle(alert.title)
                .setText(alert.description)
                .also { al ->
                    al.getLayoutContainer()?.also {
                        alert.viewBinder?.bind(it)
                    }
                }
                .apply {
                    if (noAnimation) {
                        setEnterAnimation(R.anim.anim_alerter_no_anim)
                    }

                    alert.iconId?.let {
                        setIcon(it)
                    }
                    alert.actions.forEach { action ->
                        addButton(action.title, im.vector.lib.ui.styles.R.style.Widget_Vector_Button_Text_Alerter) {
                            if (action.autoClose) {
                                currentIsDismissed()
                                Alerter.hide()
                            }
                            try {
                                action.action.run()
                            } catch (e: java.lang.Exception) {
                                Timber.e("## failed to perform action")
                            }
                        }
                    }
                    setOnClickListener { _ ->
                        alert.contentAction?.let {
                            if (alert.dismissOnClick) {
                                currentIsDismissed()
                                Alerter.hide()
                            }
                            try {
                                it.run()
                            } catch (e: java.lang.Exception) {
                                Timber.e("## failed to perform action")
                            }
                        }
                    }
                }
                .setOnHideListener {
                    // called when dismissed on swipe
                    try {
                        alert.dismissedAction?.run()
                    } catch (e: java.lang.Exception) {
                        Timber.e("## failed to perform action")
                    }
                    currentIsDismissed()
                }
                .setOnShowListener {
                    handleAccessibility(activity, animate)
                }
                .enableSwipeToDismiss()
                .enableInfiniteDuration(true)
                .apply {
                    if (alert.colorInt != null) {
                        setBackgroundColorInt(alert.colorInt!!)
                    } else if (alert.colorAttribute != null) {
                        setBackgroundColorInt(ThemeUtils.getColor(activity, alert.colorAttribute!!))
                    } else {
                        setBackgroundColorRes(alert.colorRes ?: im.vector.lib.ui.styles.R.color.notification_accent_color)
                    }
                }
                .enableIconPulse(!noAnimation)
                .show()
    }

    /* a11y */
    private fun handleAccessibility(activity: Activity, giveFocus: Boolean) {
        activity.window.decorView.findViewById<View>(com.tapadoo.alerter.R.id.llAlertBackground)?.let { alertView ->
            alertView.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES

            // Add close action for a11y (same action than swipe). User can select the action by swiping on the screen vertically,
            // and double tap to perform the action
            ViewCompat.addAccessibilityAction(
                    alertView,
                    stringProvider.getString(CommonStrings.action_close)
            ) { _, _ ->
                currentIsDismissed()
                Alerter.hide()
                true
            }

            // And give focus to the alert right now, only for first display, i.e. when there is an animation.
            if (giveFocus) {
                alertView.giveAccessibilityFocus()
            }
        }
    }

    private fun currentIsDismissed() {
        // current alert has been hidden
        if (currentAlerter?.isLight == false) {
            setLightStatusBar()
        }
        currentAlerter = null
        Handler(Looper.getMainLooper()).postDelayed({
            displayNextIfPossible()
        }, 500)
    }

    private fun shouldBeDisplayedIn(alert: VectorAlert?, activity: Activity): Boolean {
        return alert != null &&
                activity !is MainActivity &&
                activity !is PinActivity &&
                activity !is SignedOutActivity &&
                activity !is AnalyticsOptInActivity &&
                activity !is ReleaseNotesActivity &&
                activity is VectorBaseActivity<*> &&
                alert.shouldBeDisplayedIn.invoke(activity)
    }
}
