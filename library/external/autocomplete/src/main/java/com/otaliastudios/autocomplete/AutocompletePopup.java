package com.otaliastudios.autocomplete;

import android.content.Context;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.PopupWindow;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StyleRes;
import androidx.core.view.ViewCompat;
import androidx.core.widget.PopupWindowCompat;

/**
 * A simplified version of andriod.widget.ListPopupWindow, which is the class used by
 * AutocompleteTextView.
 *
 * Other than being simplified, this deals with Views rather than ListViews, so the content
 * can be whatever. Lots of logic (clicks, selections etc.) has been removed because we manage that
 * in {@link AutocompletePresenter}.
 *
 */
class AutocompletePopup {
    private Context mContext;
    private ViewGroup mView;
    private int mHeight = ViewGroup.LayoutParams.WRAP_CONTENT;
    private int mWidth = ViewGroup.LayoutParams.WRAP_CONTENT;
    private int mMaxHeight = Integer.MAX_VALUE;
    private int mMaxWidth = Integer.MAX_VALUE;
    private int mUserMaxHeight = Integer.MAX_VALUE;
    private int mUserMaxWidth = Integer.MAX_VALUE;
    private int mHorizontalOffset = 0;
    private int mVerticalOffset = 0;
    private boolean mVerticalOffsetSet;
    private int mGravity = Gravity.NO_GRAVITY;
    private boolean mAlwaysVisible = false;
    private boolean mOutsideTouchable = true;
    private View mAnchorView;
    private final Rect mTempRect = new Rect();
    private boolean mModal;
    private PopupWindow mPopup;


    /**
     * Create a new, empty popup window capable of displaying items from a ListAdapter.
     * Backgrounds should be set using {@link #setBackgroundDrawable(Drawable)}.
     *
     * @param context Context used for contained views.
     */
    AutocompletePopup(@NonNull Context context) {
        super();
        mContext = context;
        mPopup = new PopupWindow(context);
        mPopup.setInputMethodMode(PopupWindow.INPUT_METHOD_NEEDED);
    }


    /**
     * Set whether this window should be modal when shown.
     *
     * <p>If a popup window is modal, it will receive all touch and key input.
     * If the user touches outside the popup window's content area the popup window
     * will be dismissed.
     * @param modal {@code true} if the popup window should be modal, {@code false} otherwise.
     */
    @SuppressWarnings("SameParameterValue")
    void setModal(boolean modal) {
        mModal = modal;
        mPopup.setFocusable(modal);
    }

    /**
     * Returns whether the popup window will be modal when shown.
     * @return {@code true} if the popup window will be modal, {@code false} otherwise.
     */
    @SuppressWarnings("unused")
    boolean isModal() {
        return mModal;
    }

    void setElevation(float elevationPx) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) mPopup.setElevation(elevationPx);
    }

    /**
     * Sets whether the drop-down should remain visible under certain conditions.
     *
     * The drop-down will occupy the entire screen below {@link #getAnchorView} regardless
     * of the size or content of the list.  {@link #getBackground()} will fill any space
     * that is not used by the list.
     * @param dropDownAlwaysVisible Whether to keep the drop-down visible.
     *
     */
    @SuppressWarnings("unused")
    void setDropDownAlwaysVisible(boolean dropDownAlwaysVisible) {
        mAlwaysVisible = dropDownAlwaysVisible;
    }

    /**
     * @return Whether the drop-down is visible under special conditions.
     */
    @SuppressWarnings("unused")
    boolean isDropDownAlwaysVisible() {
        return mAlwaysVisible;
    }

    void setOutsideTouchable(boolean outsideTouchable) {
        mOutsideTouchable = outsideTouchable;
    }

    @SuppressWarnings("WeakerAccess")
    boolean isOutsideTouchable() {
        return mOutsideTouchable && !mAlwaysVisible;
    }

    /**
     * Sets the operating mode for the soft input area.
     * @param mode The desired mode, see
     *        {@link android.view.WindowManager.LayoutParams#softInputMode}
     *        for the full list
     * @see android.view.WindowManager.LayoutParams#softInputMode
     * @see #getSoftInputMode()
     */
    void setSoftInputMode(int mode) {
        mPopup.setSoftInputMode(mode);
    }

    /**
     * Returns the current value in {@link #setSoftInputMode(int)}.
     * @see #setSoftInputMode(int)
     * @see android.view.WindowManager.LayoutParams#softInputMode
     */
    @SuppressWarnings({"WeakerAccess", "unused"})
    int getSoftInputMode() {
        return mPopup.getSoftInputMode();
    }

    /**
     * @return The background drawable for the popup window.
     */
    @SuppressWarnings({"WeakerAccess", "unused"})
    @Nullable
    Drawable getBackground() {
        return mPopup.getBackground();
    }

    /**
     * Sets a drawable to be the background for the popup window.
     * @param d A drawable to set as the background.
     */
    void setBackgroundDrawable(@Nullable Drawable d) {
        mPopup.setBackgroundDrawable(d);
    }

    /**
     * Set an animation style to use when the popup window is shown or dismissed.
     * @param animationStyle Animation style to use.
     */
    @SuppressWarnings("unused")
    void setAnimationStyle(@StyleRes int animationStyle) {
        mPopup.setAnimationStyle(animationStyle);
    }

    /**
     * Returns the animation style that will be used when the popup window is
     * shown or dismissed.
     * @return Animation style that will be used.
     */
    @SuppressWarnings("unused")
    @StyleRes
    int getAnimationStyle() {
        return mPopup.getAnimationStyle();
    }

    /**
     * Returns the view that will be used to anchor this popup.
     * @return The popup's anchor view
     */
    @SuppressWarnings("WeakerAccess")
    View getAnchorView() {
        return mAnchorView;
    }

    /**
     * Sets the popup's anchor view. This popup will always be positioned relative to
     * the anchor view when shown.
     * @param anchor The view to use as an anchor.
     */
    void setAnchorView(@NonNull View anchor) {
        mAnchorView = anchor;
    }

    /**
     * Set the horizontal offset of this popup from its anchor view in pixels.
     * @param offset The horizontal offset of the popup from its anchor.
     */
    @SuppressWarnings("unused")
    void setHorizontalOffset(int offset) {
        mHorizontalOffset = offset;
    }

    /**
     * Set the vertical offset of this popup from its anchor view in pixels.
     * @param offset The vertical offset of the popup from its anchor.
     */
    void setVerticalOffset(int offset) {
        mVerticalOffset = offset;
        mVerticalOffsetSet = true;
    }

    /**
     * Set the gravity of the dropdown list. This is commonly used to
     * set gravity to START or END for alignment with the anchor.
     * @param gravity Gravity value to use
     */
    void setGravity(int gravity) {
        mGravity = gravity;
    }

    /**
     * @return The width of the popup window in pixels.
     */
    @SuppressWarnings("unused")
    int getWidth() {
        return mWidth;
    }

    /**
     * Sets the width of the popup window in pixels. Can also be MATCH_PARENT
     * or WRAP_CONTENT.
     * @param width Width of the popup window.
     */
    void setWidth(int width) {
        mWidth = width;
    }

    /**
     * Sets the width of the popup window by the size of its content. The final width may be
     * larger to accommodate styled window dressing.
     * @param width Desired width of content in pixels.
     */
    @SuppressWarnings("unused")
    void setContentWidth(int width) {
        Drawable popupBackground = mPopup.getBackground();
        if (popupBackground != null) {
            popupBackground.getPadding(mTempRect);
            width += mTempRect.left + mTempRect.right;
        }
        setWidth(width);
    }

    void setMaxWidth(int width) {
        if (width > 0) {
            mUserMaxWidth = width;
        }
    }

    /**
     * @return The height of the popup window in pixels.
     */
    @SuppressWarnings("unused")
    int getHeight() {
        return mHeight;
    }

    /**
     * Sets the height of the popup window in pixels. Can also be MATCH_PARENT.
     * @param height Height of the popup window.
     */
    void setHeight(int height) {
        mHeight = height;
    }

    /**
     * Sets the height of the popup window by the size of its content. The final height may be
     * larger to accommodate styled window dressing.
     * @param height Desired height of content in pixels.
     */
    @SuppressWarnings("unused")
    void setContentHeight(int height) {
        Drawable popupBackground = mPopup.getBackground();
        if (popupBackground != null) {
            popupBackground.getPadding(mTempRect);
            height += mTempRect.top + mTempRect.bottom;
        }
        setHeight(height);
    }

    void setMaxHeight(int height) {
        if (height > 0) {
            mUserMaxHeight = height;
        }
    }

    void setOnDismissListener(PopupWindow.OnDismissListener listener) {
        mPopup.setOnDismissListener(listener);
    }

    /**
     * Show the popup list. If the list is already showing, this method
     * will recalculate the popup's size and position.
     */
    void show() {
        if (!ViewCompat.isAttachedToWindow(getAnchorView())) return;

        int height = buildDropDown();
        final boolean noInputMethod = isInputMethodNotNeeded();
        int mDropDownWindowLayoutType = WindowManager.LayoutParams.TYPE_APPLICATION_SUB_PANEL;
        PopupWindowCompat.setWindowLayoutType(mPopup, mDropDownWindowLayoutType);

        if (mPopup.isShowing()) {
            // First pass for this special case, don't know why.
            if (mHeight == ViewGroup.LayoutParams.MATCH_PARENT) {
                int tempWidth = mWidth == ViewGroup.LayoutParams.MATCH_PARENT ? ViewGroup.LayoutParams.MATCH_PARENT : 0;
                if (noInputMethod) {
                    mPopup.setWidth(tempWidth);
                    mPopup.setHeight(0);
                } else {
                    mPopup.setWidth(tempWidth);
                    mPopup.setHeight(ViewGroup.LayoutParams.MATCH_PARENT);
                }
            }

            // The call to PopupWindow's update method below can accept -1
            // for any value you do not want to update.

            // Width.
            int widthSpec;
            if (mWidth == ViewGroup.LayoutParams.MATCH_PARENT) {
                widthSpec = -1;
            } else if (mWidth == ViewGroup.LayoutParams.WRAP_CONTENT) {
                widthSpec = getAnchorView().getWidth();
            } else {
                widthSpec = mWidth;
            }
            widthSpec = Math.min(widthSpec, mMaxWidth);
            widthSpec = (widthSpec < 0) ? - 1 : widthSpec;

            // Height.
            int heightSpec;
            if (mHeight == ViewGroup.LayoutParams.MATCH_PARENT) {
                heightSpec = noInputMethod ? height : ViewGroup.LayoutParams.MATCH_PARENT;
            } else if (mHeight == ViewGroup.LayoutParams.WRAP_CONTENT) {
                heightSpec = height;
            } else {
                heightSpec = mHeight;
            }
            heightSpec = Math.min(heightSpec, mMaxHeight);
            heightSpec = (heightSpec < 0) ? - 1 : heightSpec;

            // Update.
            mPopup.setOutsideTouchable(isOutsideTouchable());
            if (heightSpec == 0) {
                dismiss();
            } else {
                mPopup.update(getAnchorView(), mHorizontalOffset, mVerticalOffset, widthSpec, heightSpec);
            }

        } else {
            int widthSpec;
            if (mWidth == ViewGroup.LayoutParams.MATCH_PARENT) {
                widthSpec = ViewGroup.LayoutParams.MATCH_PARENT;
            } else if (mWidth == ViewGroup.LayoutParams.WRAP_CONTENT) {
                widthSpec = getAnchorView().getWidth();
            } else {
                widthSpec = mWidth;
            }
            widthSpec = Math.min(widthSpec, mMaxWidth);

            int heightSpec;
            if (mHeight == ViewGroup.LayoutParams.MATCH_PARENT) {
                heightSpec = ViewGroup.LayoutParams.MATCH_PARENT;
            } else if (mHeight == ViewGroup.LayoutParams.WRAP_CONTENT) {
                heightSpec = height;
            } else {
                heightSpec = mHeight;
            }
            heightSpec = Math.min(heightSpec, mMaxHeight);

            // Set width and height.
            mPopup.setWidth(widthSpec);
            mPopup.setHeight(heightSpec);
            mPopup.setClippingEnabled(true);

            // use outside touchable to dismiss drop down when touching outside of it, so
            // only set this if the dropdown is not always visible
            mPopup.setOutsideTouchable(isOutsideTouchable());
            PopupWindowCompat.showAsDropDown(mPopup, getAnchorView(), mHorizontalOffset, mVerticalOffset, mGravity);
        }
    }

    /**
     * Dismiss the popup window.
     */
    void dismiss() {
        mPopup.dismiss();
        mPopup.setContentView(null);
        mView = null;
    }

    /**
     * Control how the popup operates with an input method: one of
     * INPUT_METHOD_FROM_FOCUSABLE, INPUT_METHOD_NEEDED,
     * or INPUT_METHOD_NOT_NEEDED.
     *
     * <p>If the popup is showing, calling this method will take effect only
     * the next time the popup is shown or through a manual call to the {@link #show()}
     * method.</p>
     *
     * @see #show()
     */
    void setInputMethodMode(int mode) {
        mPopup.setInputMethodMode(mode);
    }


    /**
     * @return {@code true} if the popup is currently showing, {@code false} otherwise.
     */
    boolean isShowing() {
        return mPopup.isShowing();
    }

    /**
     * @return {@code true} if this popup is configured to assume the user does not need
     * to interact with the IME while it is showing, {@code false} otherwise.
     */
    @SuppressWarnings("WeakerAccess")
    boolean isInputMethodNotNeeded() {
        return mPopup.getInputMethodMode() == PopupWindow.INPUT_METHOD_NOT_NEEDED;
    }


    void setView(ViewGroup view) {
        mView = view;
        mView.setFocusable(true);
        mView.setFocusableInTouchMode(true);
        ViewGroup dropDownView = mView;
        mPopup.setContentView(dropDownView);
        ViewGroup.LayoutParams params = mView.getLayoutParams();
        if (params != null) {
            if (params.height > 0) setHeight(params.height);
            if (params.width > 0) setWidth(params.width);
        }
    }

    /**
     * <p>Builds the popup window's content and returns the height the popup
     * should have. Returns -1 when the content already exists.</p>
     *
     * @return the content's wrap content height or -1 if content already exists
     */
    private int buildDropDown() {
        int otherHeights = 0;

        // getMaxAvailableHeight() subtracts the padding, so we put it back
        // to get the available height for the whole window.
        final int paddingVert;
        final int paddingHoriz;
        final Drawable background = mPopup.getBackground();
        if (background != null) {
            background.getPadding(mTempRect);
            paddingVert = mTempRect.top + mTempRect.bottom;
            paddingHoriz = mTempRect.left + mTempRect.right;

            // If we don't have an explicit vertical offset, determine one from
            // the window background so that content will line up.
            if (!mVerticalOffsetSet) {
                mVerticalOffset = -mTempRect.top;
            }
        } else {
            mTempRect.setEmpty();
            paddingVert = 0;
            paddingHoriz = 0;
        }

        // Redefine dimensions taking into account maxWidth and maxHeight.
        final boolean ignoreBottomDecorations = mPopup.getInputMethodMode() == PopupWindow.INPUT_METHOD_NOT_NEEDED;
        final int maxContentHeight = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N ?
                mPopup.getMaxAvailableHeight(getAnchorView(), mVerticalOffset, ignoreBottomDecorations) :
                mPopup.getMaxAvailableHeight(getAnchorView(), mVerticalOffset);
        final int maxContentWidth = mContext.getResources().getDisplayMetrics().widthPixels - paddingHoriz;

        mMaxHeight = Math.min(maxContentHeight + paddingVert, mUserMaxHeight);
        mMaxWidth = Math.min(maxContentWidth + paddingHoriz, mUserMaxWidth);
        // if (mHeight > 0) mHeight = Math.min(mHeight, maxContentHeight);
        // if (mWidth > 0) mWidth = Math.min(mWidth, maxContentWidth);

        if (mAlwaysVisible || mHeight == ViewGroup.LayoutParams.MATCH_PARENT) {
            return mMaxHeight;
        }

        final int childWidthSpec;
        switch (mWidth) {
            case ViewGroup.LayoutParams.WRAP_CONTENT:
                childWidthSpec = View.MeasureSpec.makeMeasureSpec(maxContentWidth, View.MeasureSpec.AT_MOST); break;
            case ViewGroup.LayoutParams.MATCH_PARENT:
                childWidthSpec = View.MeasureSpec.makeMeasureSpec(maxContentWidth, View.MeasureSpec.EXACTLY); break;
            default:
                //noinspection Range
                childWidthSpec = View.MeasureSpec.makeMeasureSpec(mWidth, View.MeasureSpec.EXACTLY); break;
        }

        // Add padding only if the list has items in it, that way we don't show
        // the popup if it is not needed. For this reason, we measure as wrap_content.
        mView.measure(childWidthSpec, View.MeasureSpec.makeMeasureSpec(maxContentHeight, View.MeasureSpec.AT_MOST));
        final int viewHeight = mView.getMeasuredHeight();
        if (viewHeight > 0) {
            otherHeights += paddingVert + mView.getPaddingTop() + mView.getPaddingBottom();
        }

        return Math.min(viewHeight + otherHeights, mMaxHeight);
    }


}