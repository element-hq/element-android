/* 
 * Copyright 2014 OpenMarket Ltd
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
package im.vector.matrix.android.internal.legacy.util;

import android.os.Handler;
import android.os.Looper;

/**
 * Override the os handler to support unitary tests
 */
public class MXOsHandler {

    // the internal handler
    private final android.os.Handler mHandler;

    /**
     * Listener
     */
    public interface IPostListener {
        // a post has been done in the thread
        void onPost(Looper looper);
    }

    // static
    public static final IPostListener mPostListener = null;

    /**
     * Constructor
     *
     * @param looper the looper
     */
    public MXOsHandler(Looper looper) {
        mHandler = new Handler(looper);
    }

    /**
     * Post a runnable
     *
     * @param r the runnable
     * @return true if the runnable is placed
     */
    public boolean post(Runnable r) {
        boolean result = mHandler.post(r);

        if (result && (null != mPostListener)) {
            mPostListener.onPost(mHandler.getLooper());
        }

        return result;
    }
}
