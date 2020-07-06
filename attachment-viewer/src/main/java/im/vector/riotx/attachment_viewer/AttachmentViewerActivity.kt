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

package im.vector.riotx.attachment_viewer

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.viewpager2.widget.ViewPager2
import kotlinx.android.synthetic.main.activity_attachment_viewer.*
import kotlin.math.abs

abstract class AttachmentViewerActivity : AppCompatActivity() {

    lateinit var pager2: ViewPager2
    lateinit var imageTransitionView: ImageView
    lateinit var transitionImageContainer: ViewGroup

    var topInset = 0

    private var overlayView: View? = null
        set(value) {
            if (value == overlayView) return
            overlayView?.let { rootContainer.removeView(it) }
            rootContainer.addView(value)
            value?.updatePadding(top = topInset)
            field = value
        }

    private lateinit var swipeDismissHandler: SwipeToDismissHandler
    private lateinit var directionDetector: SwipeDirectionDetector
    private lateinit var scaleDetector: ScaleGestureDetector


    var currentPosition = 0

    private var swipeDirection: SwipeDirection? = null

    private fun isScaled() = attachmentsAdapter.isScaled(currentPosition)

    private var wasScaled: Boolean = false
    private var isSwipeToDismissAllowed: Boolean = true
    private lateinit var attachmentsAdapter: AttachmentsAdapter
    private var isOverlayWasClicked = false

//    private val shouldDismissToBottom: Boolean
//        get() = e == null
//                || !externalTransitionImageView.isRectVisible
//                || !isAtStartPosition

    private var isImagePagerIdle = true

    fun setSourceProvider(sourceProvider: AttachmentSourceProvider) {
        attachmentsAdapter.attachmentSourceProvider = sourceProvider
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // This is important for the dispatchTouchEvent, if not we must correct
        // the touch coordinates
        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)

//        // clear FLAG_TRANSLUCENT_STATUS flag:
//        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
//
//// add FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS flag to the window
//        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);


        setContentView(R.layout.activity_attachment_viewer)
        attachmentPager.orientation = ViewPager2.ORIENTATION_HORIZONTAL
        attachmentsAdapter = AttachmentsAdapter()
        attachmentPager.adapter = attachmentsAdapter
        imageTransitionView = transitionImageView
        transitionImageContainer = findViewById(R.id.transitionImageContainer)
        pager2 = attachmentPager
        directionDetector = createSwipeDirectionDetector()

        attachmentPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageScrollStateChanged(state: Int) {
                isImagePagerIdle = state == ViewPager2.SCROLL_STATE_IDLE
            }

            override fun onPageSelected(position: Int) {
                currentPosition = position
                overlayView = attachmentsAdapter.attachmentSourceProvider?.overlayViewAtPosition(this@AttachmentViewerActivity, position)
            }
        })

        swipeDismissHandler = createSwipeToDismissHandler()
        rootContainer.setOnTouchListener(swipeDismissHandler)
        rootContainer.viewTreeObserver.addOnGlobalLayoutListener { swipeDismissHandler.translationLimit = dismissContainer.height / 4 }

        scaleDetector = createScaleGestureDetector()


        ViewCompat.setOnApplyWindowInsetsListener(rootContainer) { _, insets ->
            overlayView?.updatePadding(top = insets.systemWindowInsetTop)
            topInset = insets.systemWindowInsetTop
            insets
        }

    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {

        // The zoomable view is configured to disallow interception when image is zoomed

        // Check if the overlay is visible, and wants to handle the click
        if (overlayView?.isVisible == true && overlayView?.dispatchTouchEvent(ev) == true) {
            return true
        }


        Log.v("ATTACHEMENTS", "================\ndispatchTouchEvent $ev")
        handleUpDownEvent(ev)

        Log.v("ATTACHEMENTS", "scaleDetector is in progress ${scaleDetector.isInProgress}")
        Log.v("ATTACHEMENTS", "pointerCount ${ev.pointerCount}")
        Log.v("ATTACHEMENTS", "wasScaled ${wasScaled}")
        if (swipeDirection == null && (scaleDetector.isInProgress || ev.pointerCount > 1 || wasScaled)) {
            wasScaled = true
            Log.v("ATTACHEMENTS", "dispatch to pager")
            return attachmentPager.dispatchTouchEvent(ev)
        }


        Log.v("ATTACHEMENTS", "is current item scaled ${isScaled()}")
        return (if (isScaled()) super.dispatchTouchEvent(ev) else handleTouchIfNotScaled(ev)).also {
            Log.v("ATTACHEMENTS", "\n================")
        }
    }

    private fun handleUpDownEvent(event: MotionEvent) {
        Log.v("ATTACHEMENTS", "handleUpDownEvent $event")
        if (event.action == MotionEvent.ACTION_UP) {
            handleEventActionUp(event)
        }

        if (event.action == MotionEvent.ACTION_DOWN) {
            handleEventActionDown(event)
        }

        scaleDetector.onTouchEvent(event)
//        gestureDetector.onTouchEvent(event)
    }

    private fun handleEventActionDown(event: MotionEvent) {
        swipeDirection = null
        wasScaled = false
        attachmentPager.dispatchTouchEvent(event)

        swipeDismissHandler.onTouch(rootContainer, event)
        isOverlayWasClicked = dispatchOverlayTouch(event)
    }

    private fun handleEventActionUp(event: MotionEvent) {
//        wasDoubleTapped = false
        swipeDismissHandler.onTouch(rootContainer, event)
        attachmentPager.dispatchTouchEvent(event)
        isOverlayWasClicked = dispatchOverlayTouch(event)
    }

    private fun handleTouchIfNotScaled(event: MotionEvent): Boolean {

        Log.v("ATTACHEMENTS", "handleTouchIfNotScaled ${event}")
        directionDetector.handleTouchEvent(event)

        return when (swipeDirection) {
            SwipeDirection.Up, SwipeDirection.Down -> {
                if (isSwipeToDismissAllowed && !wasScaled && isImagePagerIdle) {
                    swipeDismissHandler.onTouch(rootContainer, event)
                } else true
            }
            SwipeDirection.Left, SwipeDirection.Right -> {
                attachmentPager.dispatchTouchEvent(event)
            }
            else -> true
        }
    }


    private fun handleSwipeViewMove(translationY: Float, translationLimit: Int) {
        val alpha = calculateTranslationAlpha(translationY, translationLimit)
        backgroundView.alpha = alpha
        dismissContainer.alpha = alpha
        overlayView?.alpha = alpha
    }

    private fun dispatchOverlayTouch(event: MotionEvent): Boolean =
            overlayView
                    ?.let { it.isVisible && it.dispatchTouchEvent(event) }
                    ?: false

    private fun calculateTranslationAlpha(translationY: Float, translationLimit: Int): Float =
            1.0f - 1.0f / translationLimit.toFloat() / 4f * abs(translationY)

    private fun createSwipeToDismissHandler()
            : SwipeToDismissHandler = SwipeToDismissHandler(
            swipeView = dismissContainer,
            shouldAnimateDismiss = { shouldAnimateDismiss() },
            onDismiss = { animateClose() },
            onSwipeViewMove = ::handleSwipeViewMove)

    private fun createSwipeDirectionDetector() =
            SwipeDirectionDetector(this) { swipeDirection = it }

    private fun createScaleGestureDetector() =
            ScaleGestureDetector(this, ScaleGestureDetector.SimpleOnScaleGestureListener())


    protected open fun shouldAnimateDismiss(): Boolean = true

    protected open fun animateClose() {
        window.statusBarColor = Color.TRANSPARENT
        finish()
    }
}
