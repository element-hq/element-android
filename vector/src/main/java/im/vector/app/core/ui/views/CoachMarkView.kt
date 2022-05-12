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

package im.vector.app.core.ui.views

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.transition.Fade
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.core.view.isVisible
import im.vector.app.R

/***
 * Coach mark widget, which could be shown against any part of layout to provide user with valuable information about it
 * It shows a popup dialog which covers entire screen and consume first click on any location on the screen to dismiss itself
 * @param context the context
 * @param root fragment or activity root view to host popup dialog
 */

class CoachMarkView(val context: Context, val root: View) {

    @SuppressLint("InflateParams")
    private val view: View = LayoutInflater.from(context).inflate(R.layout.coach_mark, null)
    private val markView: ViewGroup = view.findViewById(R.id.coach_mark)

    companion object {
        /**
         * Coach mark pointers should not be positioned on top of rounded corners to prevent gaps between background shape and pointer
         * This constant must match `radius` property of coach mark's background shape
         * */
        private const val ANCHOR_MIN_HORIZONTAL_MARGIN_DP = 12f
    }

    /***
     * Show coach mark for specified anchor view
     *
     * @param stringId string resource id for text to be shown
     * @param anchor view which will be pointed by coach mark
     * @param gravity coach mark gravity. [Gravity.BOTTOM] to show mark below anchor, [Gravity.TOP] to show above.
     */
    fun show(@StringRes stringId: Int, anchor: View, gravity: Int = Gravity.NO_GRAVITY, onDismiss: (() -> Unit)? = null) {
        show(context.resources.getString(stringId), anchor, gravity, onDismiss)
    }

    /***
     * Show coach mark for specified anchor view
     *
     * @param text text to be shown
     * @param anchor view which will be pointed by coach mark
     * @param gravity coach mark gravity. [Gravity.BOTTOM] to show mark below anchor, [Gravity.TOP] to show above.
     */
    fun show(text: CharSequence, anchor: View, gravity: Int = Gravity.NO_GRAVITY, onDismiss: (() -> Unit)? = null) {
        view.findViewById<TextView>(R.id.coach_mark_content).text = text

        val width = context.resources.displayMetrics.widthPixels - markView.paddingLeft - markView.paddingRight

        markView.measure(
                View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(context.resources.displayMetrics.heightPixels, View.MeasureSpec.AT_MOST)
        )

        val anchorDimens = getAnchorDimens(anchor, root)

        setMarkVerticalPosition(context, anchorDimens, view, markView.measuredHeight, gravity)
        setPointerHorizontalPosition(anchorDimens, markView)

        createPopupWindow(view, onDismiss).showAtLocation(root, Gravity.NO_GRAVITY, 0, 0)
    }

    private fun setMarkVerticalPosition(context: Context, anchorDimens: Dimens, view: View, markHeight: Int, gravity: Int) {
        val maxHeight = context.resources.displayMetrics.heightPixels

        val resolvedGravity = resolveGravity(
                maxHeight = maxHeight,
                anchorDimens = anchorDimens,
                markHeight = markHeight,
                gravity = gravity
        )

        view.findViewById<View>(R.id.coach_mark_arrow_top).isVisible = resolvedGravity == Gravity.BOTTOM
        view.findViewById<View>(R.id.coach_mark_arrow_bottom).isVisible = resolvedGravity == Gravity.TOP

        val markY = when (resolvedGravity) {
            Gravity.TOP    -> anchorDimens.y - markHeight
            Gravity.BOTTOM -> anchorDimens.y + anchorDimens.height
            else           -> maxHeight / 2 - markHeight / 2
        }

        setMarkY(markY, view)
    }

    private fun resolveGravity(maxHeight: Int, anchorDimens: Dimens, markHeight: Int, gravity: Int): Int {
        val topSpace = anchorDimens.y
        val bottomSpace = maxHeight - (anchorDimens.y + anchorDimens.height)

        val fitAbove = topSpace >= markHeight
        val fitBelow = bottomSpace >= markHeight

        if (!fitAbove && !fitBelow) {
            return Gravity.CENTER
        }

        return when {
            gravity == Gravity.TOP && fitAbove    -> Gravity.TOP
            gravity == Gravity.BOTTOM && fitBelow -> Gravity.BOTTOM
            bottomSpace >= topSpace               -> Gravity.BOTTOM //choose the bigger side if required gravity isn't provided or can't be applied
            else                                  -> Gravity.TOP
        }
    }

    private fun setMarkY(y: Int, markView: View) {
        val mark = markView.findViewById<View>(R.id.coach_mark)
        val params = (mark.layoutParams as ViewGroup.MarginLayoutParams)
        params.setMargins(0, y, 0, 0)
        mark.layoutParams = params
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun createPopupWindow(containerView: View, onDismiss: (() -> Unit)?): PopupWindow =
            PopupWindow(containerView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                    .apply {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            enterTransition = Fade(Fade.IN)
                            exitTransition = Fade(Fade.OUT)
                        }

                        isTouchable = true
                        setTouchInterceptor { _, _ ->
                            dismiss()
                            onDismiss?.invoke()
                            true
                        }
                    }

    private fun setPointerHorizontalPosition(anchorDimens: Dimens, markView: View) {
        val topArrow = markView.findViewById<View>(R.id.coach_mark_arrow_top)
        val bottomArrow = markView.findViewById<View>(R.id.coach_mark_arrow_bottom)

        val paddingHorizontal = markView.paddingStart
        val anchorCenter = anchorDimens.x + anchorDimens.width / 2
        val minHorizontalMargin = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                ANCHOR_MIN_HORIZONTAL_MARGIN_DP,
                markView.context.resources.displayMetrics
        ).toInt()

        var leftMargin = anchorCenter - paddingHorizontal - topArrow.measuredWidth / 2
        leftMargin = leftMargin.coerceAtLeast(minHorizontalMargin)

        (topArrow.layoutParams as ViewGroup.MarginLayoutParams).leftMargin = leftMargin
        (bottomArrow.layoutParams as ViewGroup.MarginLayoutParams).leftMargin = leftMargin
    }

    private fun getAnchorDimens(anchor: View, parent: View): Dimens {
        val rootViewLoc = IntArray(2)
        parent.getLocationOnScreen(rootViewLoc)

        val anchorLoc = IntArray(2)
        anchor.getLocationOnScreen(anchorLoc)
        anchorLoc[1] -= rootViewLoc[1]

        return Dimens(anchorLoc[0], anchorLoc[1], anchor.measuredWidth, anchor.measuredHeight)
    }

    private data class Dimens(val x: Int, val y: Int, val width: Int, val height: Int)
}
