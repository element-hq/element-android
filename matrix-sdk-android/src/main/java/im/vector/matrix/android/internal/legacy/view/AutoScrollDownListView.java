/* 
 * Copyright 2014 OpenMarket Ltd
 * Copyright 2018 New Vector Ltd
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
package im.vector.matrix.android.internal.legacy.view;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ListView;

import im.vector.matrix.android.internal.legacy.util.Log;

/**
 * The listView automatically scrolls down when its height is updated.
 * It is used to scroll the list when the keyboard is displayed
 * Note that the list scrolls down automatically thank to android:transcriptMode="normal" in the XML
 */
public class AutoScrollDownListView extends ListView {
    private static final String LOG_TAG = AutoScrollDownListView.class.getSimpleName();

    private boolean mLockSelectionOnResize = false;

    public AutoScrollDownListView(Context context) {
        super(context);
    }

    public AutoScrollDownListView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AutoScrollDownListView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onSizeChanged(int xNew, int yNew, int xOld, int yOld) {
        super.onSizeChanged(xNew, yNew, xOld, yOld);

        if (!mLockSelectionOnResize) {
            // check if the keyboard is displayed
            // we don't want that the list scrolls to the bottom when the keyboard is hidden.
            if (yNew < yOld) {
                postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        setSelection(getCount() - 1);
                    }
                }, 100);
            }
        }
    }

    /**
     * The listview selection is locked even if the view position is updated.
     */
    public void lockSelectionOnResize() {
        mLockSelectionOnResize = true;
    }

    @Override
    protected void layoutChildren() {
        // the adapter items are added without refreshing the list (back pagination only)
        // to reduce the number of refresh
        try {
            super.layoutChildren();
        } catch (Exception e) {
            Log.e(LOG_TAG, "## layoutChildren() failed " + e.getMessage(), e);
        }
    }

    @Override
    // require to avoid lint errors with MatrixMessageListFragment
    public void setSelectionFromTop(int position, int y) {
        super.setSelectionFromTop(position, y);
    }
}
