package de.spiritcroc.recyclerview.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import androidx.recyclerview.widget.RecyclerView;

/**
 * Exposing/replicating some internal functions/attributes from RecylerView.LayoutManager
 */
public abstract class LinearLayoutManager extends androidx.recyclerview.widget.LinearLayoutManager {

    public LinearLayoutManager(Context context) {
        super(context);
    }

    public LinearLayoutManager(Context context, int orientation, boolean reverseLayout) {
        super(context, orientation, reverseLayout);
    }

    public LinearLayoutManager(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    /*
     * Exposed things from RecyclerView.java
     */

    /**
     * The callback used for retrieving information about a RecyclerView and its children in the
     * horizontal direction.
     */
    private final ViewBoundsCheck.Callback mHorizontalBoundCheckCallback =
            new ViewBoundsCheck.Callback() {
                @Override
                public View getChildAt(int index) {
                    return LinearLayoutManager.this.getChildAt(index);
                }

                @Override
                public int getParentStart() {
                    return LinearLayoutManager.this.getPaddingLeft();
                }

                @Override
                public int getParentEnd() {
                    return LinearLayoutManager.this.getWidth() - LinearLayoutManager.this.getPaddingRight();
                }

                @Override
                public int getChildStart(View view) {
                    final RecyclerView.LayoutParams params = (RecyclerView.LayoutParams)
                            view.getLayoutParams();
                    return LinearLayoutManager.this.getDecoratedLeft(view) - params.leftMargin;
                }

                @Override
                public int getChildEnd(View view) {
                    final RecyclerView.LayoutParams params = (RecyclerView.LayoutParams)
                            view.getLayoutParams();
                    return LinearLayoutManager.this.getDecoratedRight(view) + params.rightMargin;
                }
            };

    /**
     * The callback used for retrieving information about a RecyclerView and its children in the
     * vertical direction.
     */
    private final ViewBoundsCheck.Callback mVerticalBoundCheckCallback =
            new ViewBoundsCheck.Callback() {
                @Override
                public View getChildAt(int index) {
                    return LinearLayoutManager.this.getChildAt(index);
                }

                @Override
                public int getParentStart() {
                    return LinearLayoutManager.this.getPaddingTop();
                }

                @Override
                public int getParentEnd() {
                    return LinearLayoutManager.this.getHeight()
                            - LinearLayoutManager.this.getPaddingBottom();
                }

                @Override
                public int getChildStart(View view) {
                    final RecyclerView.LayoutParams params = (RecyclerView.LayoutParams)
                            view.getLayoutParams();
                    return LinearLayoutManager.this.getDecoratedTop(view) - params.topMargin;
                }

                @Override
                public int getChildEnd(View view) {
                    final RecyclerView.LayoutParams params = (RecyclerView.LayoutParams)
                            view.getLayoutParams();
                    return LinearLayoutManager.this.getDecoratedBottom(view) + params.bottomMargin;
                }
            };

    /**
     * Utility objects used to check the boundaries of children against their parent
     * RecyclerView.
     *
     * @see #isViewPartiallyVisible(View, boolean, boolean),
     * {@link LinearLayoutManager#findOneVisibleChild(int, int, boolean, boolean)},
     * and {@link LinearLayoutManager#findOnePartiallyOrCompletelyInvisibleChild(int, int)}.
     */
    ViewBoundsCheck mHorizontalBoundCheck = new ViewBoundsCheck(mHorizontalBoundCheckCallback);
    ViewBoundsCheck mVerticalBoundCheck = new ViewBoundsCheck(mVerticalBoundCheckCallback);

}
