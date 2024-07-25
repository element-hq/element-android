package im.vector.app.core.utils

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.View.MeasureSpec
import android.view.ViewGroup
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsAnimationCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.customview.widget.ViewDragHelper
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.bottomsheet.BottomSheetBehavior
import timber.log.Timber
import java.lang.ref.WeakReference
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * BottomSheetBehavior that dynamically resizes its contents as it grows or shrinks.
 * Most of the nested scrolling and touch events code is the same as in [BottomSheetBehavior], but we couldn't just extend it.
 */
class ExpandingBottomSheetBehavior<V : View> : CoordinatorLayout.Behavior<V> {

    companion object {
        /** Gets a [ExpandingBottomSheetBehavior] from the passed [view] if it exists. */
        @Suppress("UNCHECKED_CAST")
        fun <V : View> from(view: V): ExpandingBottomSheetBehavior<V>? {
            val params = view.layoutParams as? CoordinatorLayout.LayoutParams ?: return null
            return params.behavior as? ExpandingBottomSheetBehavior<V>
        }
    }

    /** [Callback] to notify changes in dragging state and position. */
    interface Callback {
        /** Called when the dragging state of the BottomSheet changes. */
        fun onStateChanged(state: State) {}

        /** Called when the position of the BottomSheet changes while dragging. */
        fun onSlidePositionChanged(view: View, yPosition: Float) {}
    }

    /** Represents the 4 possible states of the BottomSheet. */
    enum class State(val value: Int) {
        /** BottomSheet is at min height, collapsed at the bottom. */
        Collapsed(0),

        /** BottomSheet is being dragged by the user. */
        Dragging(1),

        /** BottomSheet has been released after being dragged by the user and is animating to its destination. */
        Settling(2),

        /** BottomSheet is at its max height. */
        Expanded(3);

        /** Returns whether the BottomSheet is being dragged or is settling after being dragged. */
        fun isDraggingOrSettling(): Boolean = this == Dragging || this == Settling
    }

    /** Set to true to enable debug logging of sizes and offsets. Defaults to `false`. */
    var enableDebugLogs = false

    /** Current BottomSheet state. Default to [State.Collapsed]. */
    var state: State = State.Collapsed
        private set

    /** Whether the BottomSheet can be dragged by the user or not. Defaults to `true`. */
    var isDraggable = true

    /** [Callback] to notify changes in dragging state and position. */
    var callback: Callback? = null
        set(value) {
            field = value
            // Send initial state
            value?.onStateChanged(state)
        }

    /** Additional top offset in `dps` to add to the BottomSheet so it doesn't fill the whole screen. Defaults to `0`. */
    var topOffset = 0
        set(value) {
            field = value
            expandedOffset = -1
        }

    /** Whether the BottomSheet should be expanded up to the bottom of any [AppBarLayout] found in the parent [CoordinatorLayout]. Defaults to `false`. */
    var avoidAppBarLayout = false
        set(value) {
            field = value
            expandedOffset = -1
        }

    /**
     * Whether to add the [scrimView], a 'shadow layer' that will be displayed while dragging/expanded so it obscures the content below the BottomSheet.
     * Defaults to `false`.
     */
    var useScrimView = false

    /** Color to use for the [scrimView] shadow layer. */
    var scrimViewColor = 0x60000000

    /** [View.TRANSLATION_Z] in `dps` to apply to the [scrimView]. Defaults to `0dp`. */
    var scrimViewTranslationZ = 0

    /** Whether the content view should be layout to the top of the BottomSheet when it's collapsed. Defaults to true. */
    var applyInsetsToContentViewWhenCollapsed = true

    /** Lambda used to calculate a min collapsed when the view using the behavior should have a special 'collapsed' layout. It's null by default. */
    var minCollapsedHeight: (() -> Int)? = null

    // Internal BottomSheet implementation properties
    private var ignoreEvents = false
    private var touchingScrollingChild = false

    private var lastY: Int = -1
    private var collapsedOffset = -1
    private var expandedOffset = -1
    private var parentWidth = -1
    private var parentHeight = -1

    private var activePointerId = -1

    private var lastNestedScrollDy = -1
    private var isNestedScrolled = false

    private var viewRef: WeakReference<V>? = null
    private var nestedScrollingChildRef: WeakReference<View>? = null
    private var velocityTracker: VelocityTracker? = null

    private var dragHelper: ViewDragHelper? = null
    private var scrimView: View? = null

    private val stateSettlingTracker = StateSettlingTracker()
    private var prevState: State? = null

    private var insetBottom = 0
    private var insetTop = 0
    private var insetLeft = 0
    private var insetRight = 0

    private var initialPaddingTop = 0
    private var initialPaddingBottom = 0
    private var initialPaddingLeft = 0
    private var initialPaddingRight = 0
    private val minCollapsedOffset: Int?
        get() {
            val minHeight = minCollapsedHeight?.invoke() ?: return null
            if (minHeight == -1) return null
            return parentHeight - minHeight - insetBottom
        }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor() : super()

    override fun onLayoutChild(parent: CoordinatorLayout, child: V, layoutDirection: Int): Boolean {
        parentWidth = parent.width
        parentHeight = parent.height

        if (viewRef == null) {
            viewRef = WeakReference(child)
            setWindowInsetsListener(child)
            // Prevents clicking on overlapped items below the BottomSheet
            child.isClickable = true
        }

        parent.updatePadding(left = insetLeft, right = insetRight)

        ensureViewDragHelper(parent)

        // Top coordinate before this layout pass
        val savedTop = child.top

        // Calculate default position of the BottomSheet's children
        parent.onLayoutChild(child, layoutDirection)

        // This should optimise calculations when they're not needed
        if (state == State.Collapsed) {
            calculateCollapsedOffset(child)
        }
        calculateExpandedOffset(parent)

        // Apply top and bottom insets to contentView if needed
        val appBar = findAppBarLayout(parent)
        val contentView = parent.children.find { it !== appBar && it !== child && it !== scrimView }
        if (applyInsetsToContentViewWhenCollapsed && state == State.Collapsed && contentView != null) {
            val topOffset = appBar?.measuredHeight ?: 0
            val bottomOffset = parentHeight - collapsedOffset + insetTop
            val params = contentView.layoutParams as CoordinatorLayout.LayoutParams
            if (params.bottomMargin != bottomOffset || params.topMargin != topOffset) {
                params.topMargin = topOffset
                params.bottomMargin = bottomOffset
                contentView.layoutParams = params
            }
        }

        // Add scrimView if needed
        if (useScrimView && scrimView == null) {
            val scrimView = View(parent.context)
            scrimView.setBackgroundColor(scrimViewColor)
            @Suppress("DEPRECATION")
            scrimView.translationZ = scrimViewTranslationZ * child.resources.displayMetrics.scaledDensity
            scrimView.isVisible = false
            val params = CoordinatorLayout.LayoutParams(
                    CoordinatorLayout.LayoutParams.MATCH_PARENT,
                    CoordinatorLayout.LayoutParams.MATCH_PARENT
            )
            scrimView.layoutParams = params
            val currentIndex = parent.children.indexOf(child)
            parent.addView(scrimView, currentIndex)
            this.scrimView = scrimView
        } else if (!useScrimView && scrimView != null) {
            parent.removeView(scrimView)
            scrimView = null
        }

        // Apply insets and resize child based on the current State
        when (state) {
            State.Collapsed -> {
                scrimView?.alpha = 0f
                val newHeight = parentHeight - collapsedOffset + insetTop
                val params = child.layoutParams
                if (params.height != newHeight) {
                    params.height = newHeight
                    child.layoutParams = params
                }
                // If the offset is < insetTop it will cover the status bar too
                val newOffset = max(insetTop, collapsedOffset - insetTop)
                ViewCompat.offsetTopAndBottom(child, newOffset)
                log("State: Collapsed | Offset: $newOffset | Height: $newHeight")
            }
            State.Dragging, State.Settling -> {
                val newOffset = savedTop - child.top
                val percentage = max(0f, 1f - (newOffset.toFloat() / collapsedOffset.toFloat()))
                scrimView?.let {
                    if (percentage == 0f) {
                        it.isVisible = false
                    } else {
                        it.alpha = percentage
                        it.isVisible = true
                    }
                }
                val params = child.layoutParams
                params.height = parentHeight - savedTop
                child.layoutParams = params
                ViewCompat.offsetTopAndBottom(child, newOffset)
                val stateStr = if (state == State.Dragging) "Dragging" else "Settling"
                log("State: $stateStr | Offset: $newOffset | Percentage: $percentage")
            }
            State.Expanded -> {
                val params = child.layoutParams
                val newHeight = parentHeight - expandedOffset
                if (params.height != newHeight) {
                    params.height = newHeight
                    child.layoutParams = params
                }
                ViewCompat.offsetTopAndBottom(child, expandedOffset)
                log("State: Expanded | Offset: $expandedOffset | Height: $newHeight")
            }
        }

        // Find a nested scrolling child to take into account for touch events
        if (nestedScrollingChildRef == null) {
            nestedScrollingChildRef = findScrollingChild(child)?.let { WeakReference(it) }
        }

        return true
    }

    // region: Touch events
    override fun onInterceptTouchEvent(
            parent: CoordinatorLayout,
            child: V,
            ev: MotionEvent
    ): Boolean {
        // Almost everything inside here is verbatim to BottomSheetBehavior's onTouchEvent
        if (viewRef != null && viewRef?.get() !== child) {
            return true
        }
        val action = ev.actionMasked

        if (action == MotionEvent.ACTION_DOWN) {
            resetTouchEventTracking()
        }
        if (velocityTracker == null) {
            velocityTracker = VelocityTracker.obtain()
        }
        velocityTracker?.addMovement(ev)

        when (action) {
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                touchingScrollingChild = false
                activePointerId = MotionEvent.INVALID_POINTER_ID
                if (ignoreEvents) {
                    ignoreEvents = false
                    return false
                }
            }
            MotionEvent.ACTION_DOWN -> {
                val x = ev.x.toInt()
                lastY = ev.y.toInt()

                // Only intercept nested scrolling events here if the view not being moved by the
                // ViewDragHelper.
                val scroll = nestedScrollingChildRef?.get()
                if (state != State.Settling) {
                    if (scroll != null && parent.isPointInChildBounds(scroll, x, lastY)) {
                        activePointerId = ev.getPointerId(ev.actionIndex)
                        touchingScrollingChild = true
                    }
                }
                ignoreEvents = (activePointerId == MotionEvent.INVALID_POINTER_ID &&
                        !parent.isPointInChildBounds(child, x, lastY))
            }
            else -> Unit
        }

        if (!ignoreEvents && isDraggable && dragHelper?.shouldInterceptTouchEvent(ev) == true) {
            return true
        }

        // If using scrim view, a click on it should collapse the bottom sheet
        if (useScrimView && state == State.Expanded && action == MotionEvent.ACTION_DOWN) {
            val y = ev.y.toInt()
            if (y <= expandedOffset) {
                setState(State.Collapsed)
                return true
            }
        }

        // We have to handle cases that the ViewDragHelper does not capture the bottom sheet because
        // it is not the top most view of its parent. This is not necessary when the touch event is
        // happening over the scrolling content as nested scrolling logic handles that case.
        val scroll = nestedScrollingChildRef?.get()
        return (action == MotionEvent.ACTION_MOVE &&
                scroll != null &&
                !ignoreEvents &&
                state != State.Dragging &&
                !parent.isPointInChildBounds(scroll, ev.x.toInt(), ev.y.toInt()) &&
                dragHelper != null &&
                abs(lastY - ev.y.toInt()) > (dragHelper?.touchSlop ?: 0))
    }

    override fun onTouchEvent(parent: CoordinatorLayout, child: V, ev: MotionEvent): Boolean {
        // Almost everything inside here is verbatim to BottomSheetBehavior's onTouchEvent
        val action = ev.actionMasked
        if (state == State.Dragging && action == MotionEvent.ACTION_DOWN) {
            return true
        }
        if (shouldHandleDraggingWithHelper()) {
            dragHelper?.processTouchEvent(ev)
        }

        // Record the velocity
        if (action == MotionEvent.ACTION_DOWN) {
            resetTouchEventTracking()
        }
        if (velocityTracker == null) {
            velocityTracker = VelocityTracker.obtain()
        }
        velocityTracker?.addMovement(ev)

        if (shouldHandleDraggingWithHelper() && action == MotionEvent.ACTION_MOVE && !ignoreEvents) {
            if (abs(lastY - ev.y.toInt()) > (dragHelper?.touchSlop ?: 0)) {
                dragHelper?.captureChildView(child, ev.getPointerId(ev.actionIndex))
            }
        }

        return !ignoreEvents
    }

    private fun resetTouchEventTracking() {
        activePointerId = ViewDragHelper.INVALID_POINTER
        velocityTracker?.recycle()
        velocityTracker = null
    }
    // endregion

    override fun onAttachedToLayoutParams(params: CoordinatorLayout.LayoutParams) {
        super.onAttachedToLayoutParams(params)

        viewRef = null
        dragHelper = null
    }

    override fun onDetachedFromLayoutParams() {
        super.onDetachedFromLayoutParams()

        viewRef = null
        dragHelper = null
    }

    // region: Size measuring and utils
    private fun calculateCollapsedOffset(child: View) {
        val availableSpace = parentHeight - insetTop
        child.measure(
                MeasureSpec.makeMeasureSpec(parentWidth, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(availableSpace, MeasureSpec.AT_MOST),
        )
        collapsedOffset = parentHeight - child.measuredHeight + insetTop
    }

    private fun calculateExpandedOffset(parent: CoordinatorLayout): Int {
        expandedOffset = if (avoidAppBarLayout) {
            findAppBarLayout(parent)?.measuredHeight ?: 0
        } else {
            0
        } + topOffset + insetTop
        return expandedOffset
    }

    private fun ensureViewDragHelper(parent: CoordinatorLayout) {
        if (dragHelper == null) {
            dragHelper = ViewDragHelper.create(parent, dragHelperCallback)
        }
    }

    private fun findAppBarLayout(view: View): AppBarLayout? {
        return when (view) {
            is AppBarLayout -> view
            is ViewGroup -> view.children.firstNotNullOfOrNull { findAppBarLayout(it) }
            else -> null
        }
    }

    private fun shouldHandleDraggingWithHelper(): Boolean {
        return dragHelper != null && (isDraggable || state == State.Dragging)
    }

    private fun log(contents: String, vararg args: Any) {
        if (!enableDebugLogs) return
        Timber.d(contents, args)
    }
    // endregion

    // region: State and delayed state settling
    fun setState(state: State) {
        if (state == this.state) {
            return
        } else if (viewRef?.get() == null) {
            setInternalState(state)
        } else {
            viewRef?.get()?.let { child ->
                runAfterLayout(child) { startSettling(child, state, false) }
            }
        }
    }

    private fun setInternalState(state: State) {
        if (!this.state.isDraggingOrSettling()) {
            prevState = this.state
        }
        this.state = state

        viewRef?.get()?.requestLayout()

        callback?.onStateChanged(state)
    }

    private fun startSettling(child: View, state: State, isReleasingView: Boolean) {
        val top = getTopOffsetForState(state)
        log("Settling to: $top")
        val isSettling = dragHelper?.let {
            if (isReleasingView) {
                it.settleCapturedViewAt(child.left, top)
            } else {
                it.smoothSlideViewTo(child, child.left, top)
            }
        } ?: false
        setInternalState(if (isSettling) State.Settling else state)

        if (isSettling) {
            stateSettlingTracker.continueSettlingToState(state)
        }
    }

    private fun runAfterLayout(child: V, runnable: Runnable) {
        if (isLayouting(child)) {
            child.post(runnable)
        } else {
            runnable.run()
        }
    }

    private fun isLayouting(child: V): Boolean {
        return child.parent != null && child.parent.isLayoutRequested && child.isAttachedToWindow
    }

    private fun getTopOffsetForState(state: State): Int {
        return when (state) {
            State.Collapsed -> minCollapsedOffset ?: collapsedOffset
            State.Expanded -> expandedOffset
            else -> error("Cannot get offset for state $state")
        }
    }
    // endregion

    // region: Nested scroll
    override fun onStartNestedScroll(
            coordinatorLayout: CoordinatorLayout,
            child: V,
            directTargetChild: View,
            target: View,
            axes: Int,
            type: Int
    ): Boolean {
        lastNestedScrollDy = 0
        isNestedScrolled = false
        return (axes and ViewCompat.SCROLL_AXIS_VERTICAL) != 0
    }

    override fun onNestedPreScroll(
            coordinatorLayout: CoordinatorLayout,
            child: V,
            target: View,
            dx: Int,
            dy: Int,
            consumed: IntArray,
            type: Int
    ) {
        if (type == ViewCompat.TYPE_NON_TOUCH) return
        val scrollingChild = nestedScrollingChildRef?.get()
        if (target != scrollingChild) return

        val currentTop = child.top
        val newTop = currentTop - dy
        if (dy > 0) {
            // Upward scroll
            if (newTop < expandedOffset) {
                consumed[1] = currentTop - expandedOffset
                ViewCompat.offsetTopAndBottom(child, -consumed[1])
                setInternalState(State.Expanded)
            } else {
                if (!isDraggable) return

                consumed[1] = dy
                ViewCompat.offsetTopAndBottom(child, -dy)
                setInternalState(State.Dragging)
            }
        } else if (dy < 0) {
            // Scroll downward
            if (!target.canScrollVertically(-1)) {
                if (newTop <= collapsedOffset) {
                    if (!isDraggable) return

                    consumed[1] = dy
                    ViewCompat.offsetTopAndBottom(child, -dy)
                    setInternalState(State.Dragging)
                } else {
                    consumed[1] = currentTop - collapsedOffset
                    ViewCompat.offsetTopAndBottom(child, -consumed[1])
                    setInternalState(State.Collapsed)
                }
            }
        }
        lastNestedScrollDy = dy
        isNestedScrolled = true
    }

    override fun onNestedScroll(
            coordinatorLayout: CoordinatorLayout,
            child: V,
            target: View,
            dxConsumed: Int,
            dyConsumed: Int,
            dxUnconsumed: Int,
            dyUnconsumed: Int,
            type: Int,
            consumed: IntArray
    ) {
        // Empty to avoid default behaviour
    }

    override fun onNestedPreFling(
            coordinatorLayout: CoordinatorLayout,
            child: V,
            target: View,
            velocityX: Float,
            velocityY: Float
    ): Boolean {
        return target == nestedScrollingChildRef?.get() &&
                (state != State.Expanded || super.onNestedPreFling(coordinatorLayout, child, target, velocityX, velocityY))
    }

    private fun findScrollingChild(view: View): View? {
        return when {
            !view.isVisible -> null
            ViewCompat.isNestedScrollingEnabled(view) -> view
            view is ViewGroup -> {
                view.children.firstNotNullOfOrNull { findScrollingChild(it) }
            }
            else -> null
        }
    }
    // endregion

    // region: Insets
    private fun setWindowInsetsListener(view: View) {
        // Create a snapshot of the view's padding state.
        initialPaddingLeft = view.paddingLeft
        initialPaddingTop = view.paddingTop
        initialPaddingRight = view.paddingRight
        initialPaddingBottom = view.paddingBottom

        // This should only be used to set initial insets and other edge cases where the insets can't be applied using an animation.
        var isAnimating = false

        // This will animate inset changes, making them look a lot better. However, it won't update initial insets.
        ViewCompat.setWindowInsetsAnimationCallback(view, object : WindowInsetsAnimationCompat.Callback(DISPATCH_MODE_STOP) {
            override fun onPrepare(animation: WindowInsetsAnimationCompat) {
                isAnimating = true
            }

            override fun onProgress(insets: WindowInsetsCompat, runningAnimations: MutableList<WindowInsetsAnimationCompat>): WindowInsetsCompat {
                return if (isAnimating) {
                    applyInsets(view, insets)
                } else {
                    insets
                }
            }

            override fun onEnd(animation: WindowInsetsAnimationCompat) {
                isAnimating = false
                view.requestApplyInsets()
            }
        })

        ViewCompat.setOnApplyWindowInsetsListener(view) { _: View, insets: WindowInsetsCompat ->
            if (isAnimating) {
                insets
            } else {
                applyInsets(view, insets)
            }
        }

        // Request to apply insets as soon as the view is attached to a window.
        if (view.isAttachedToWindow) {
            ViewCompat.requestApplyInsets(view)
        } else {
            view.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
                override fun onViewAttachedToWindow(v: View) {
                    v.removeOnAttachStateChangeListener(this)
                    ViewCompat.requestApplyInsets(v)
                }

                override fun onViewDetachedFromWindow(v: View) = Unit
            })
        }
    }

    private fun applyInsets(view: View, insets: WindowInsetsCompat): WindowInsetsCompat {
        val insetsType = WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime()
        val imeInsets = insets.getInsets(insetsType)
        insetTop = imeInsets.top
        insetBottom = imeInsets.bottom
        insetLeft = imeInsets.left
        insetRight = imeInsets.right

        val bottomPadding = initialPaddingBottom + insetBottom
        view.setPadding(initialPaddingLeft, initialPaddingTop, initialPaddingRight, bottomPadding)
        if (state == State.Collapsed) {
            val params = view.layoutParams
            params.height = CoordinatorLayout.LayoutParams.WRAP_CONTENT
            view.layoutParams = params
            calculateCollapsedOffset(view)
        }
        return WindowInsetsCompat.CONSUMED
    }
    // endregion

    // Used to add dragging animations along with StateSettlingTracker, and set max and min dragging coordinates.
    private val dragHelperCallback = object : ViewDragHelper.Callback() {

        override fun tryCaptureView(child: View, pointerId: Int): Boolean {
            if (state == State.Dragging) {
                return false
            }

            if (touchingScrollingChild) {
                return false
            }

            if (state == State.Expanded && activePointerId == pointerId) {
                val scroll = nestedScrollingChildRef?.get()
                if (scroll?.canScrollVertically(-1) == true) {
                    return false
                }
            }

            return viewRef?.get() == child
        }

        override fun onViewDragStateChanged(state: Int) {
            if (state == ViewDragHelper.STATE_DRAGGING && isDraggable) {
                setInternalState(State.Dragging)
            }
        }

        override fun onViewPositionChanged(
                changedView: View,
                left: Int,
                top: Int,
                dx: Int,
                dy: Int
        ) {
            super.onViewPositionChanged(changedView, left, top, dx, dy)

            val params = changedView.layoutParams
            params.height = parentHeight - top + insetBottom + insetTop
            changedView.layoutParams = params

            val collapsedOffset = minCollapsedOffset ?: collapsedOffset
            val percentage = 1f - (top - insetTop).toFloat() / collapsedOffset.toFloat()

            callback?.onSlidePositionChanged(changedView, percentage)
        }

        override fun onViewReleased(releasedChild: View, xvel: Float, yvel: Float) {
            val actualCollapsedOffset = minCollapsedOffset ?: collapsedOffset
            val targetState = if (yvel < 0) {
                // Moving up
                val currentTop = releasedChild.top

                val yPositionPercentage = currentTop * 100f / actualCollapsedOffset
                if (yPositionPercentage >= 0.5f) {
                    State.Expanded
                } else {
                    State.Collapsed
                }
            } else if (yvel == 0f || abs(xvel) > abs(yvel)) {
                // If the Y velocity is 0 or the swipe was mostly horizontal indicated by the X velocity
                // being greater than the Y velocity, settle to the nearest correct height.

                val currentTop = releasedChild.top
                if (currentTop < actualCollapsedOffset / 2) {
                    State.Expanded
                } else {
                    State.Collapsed
                }
            } else {
                // Moving down
                val currentTop = releasedChild.top

                val yPositionPercentage = currentTop * 100f / actualCollapsedOffset
                if (yPositionPercentage >= 0.5f) {
                    State.Collapsed
                } else {
                    State.Expanded
                }
            }
            startSettling(releasedChild, targetState, true)
        }

        override fun clampViewPositionHorizontal(child: View, left: Int, dx: Int): Int {
            return child.left
        }

        override fun clampViewPositionVertical(child: View, top: Int, dy: Int): Int {
            val collapsed = minCollapsedOffset ?: collapsedOffset
            val maxTop = max(top, insetTop)
            return min(max(maxTop, expandedOffset), collapsed)
        }

        override fun getViewVerticalDragRange(child: View): Int {
            return minCollapsedOffset ?: collapsedOffset
        }
    }

    // Used to set the current State in a delayed way.
    private inner class StateSettlingTracker {
        private lateinit var targetState: State
        private var isContinueSettlingRunnablePosted = false

        private val continueSettlingRunnable: Runnable = Runnable {
            isContinueSettlingRunnablePosted = false
            if (dragHelper?.continueSettling(true) == true) {
                continueSettlingToState(targetState)
            } else {
                setInternalState(targetState)
            }
        }

        fun continueSettlingToState(state: State) {
            val view = viewRef?.get() ?: return

            this.targetState = state
            if (!isContinueSettlingRunnablePosted) {
                view.postOnAnimation(continueSettlingRunnable)
                isContinueSettlingRunnablePosted = true
            }
        }
    }
}
