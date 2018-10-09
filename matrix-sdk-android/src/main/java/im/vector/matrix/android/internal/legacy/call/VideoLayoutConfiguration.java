/*
 * Copyright 2015 OpenMarket Ltd
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

package im.vector.matrix.android.internal.legacy.call;

import java.io.Serializable;

/**
 * Defines the video call view layout.
 */
public class VideoLayoutConfiguration implements Serializable {
    public final static int INVALID_VALUE = -1;

    @Override
    public String toString() {
        return "VideoLayoutConfiguration{" +
                "mIsPortrait=" + mIsPortrait +
                ", X=" + mX +
                ", Y=" + mY +
                ", Width=" + mWidth +
                ", Height=" + mHeight +
                '}';
    }

    // parameters of the video of the local user (small video)
    /**
     * margin left in percentage of the screen resolution for the local user video
     **/
    public int mX;
    /**
     * margin top in percentage of the screen resolution for the local user video
     **/
    public int mY;

    /**
     * width in percentage of the screen resolution for the local user video
     **/
    public int mWidth;
    /**
     * video height in percentage of the screen resolution for the local user video
     **/
    public int mHeight;

    /**
     * the area size in which the video in displayed
     **/
    public int mDisplayWidth;
    public int mDisplayHeight;

    /**
     * tells if the display in is a portrait orientation
     **/
    public boolean mIsPortrait;

    public VideoLayoutConfiguration(int aX, int aY, int aWidth, int aHeight) {
        this(aX, aY, aWidth, aHeight, INVALID_VALUE, INVALID_VALUE);
    }

    public VideoLayoutConfiguration(int aX, int aY, int aWidth, int aHeight, int aDisplayWidth, int aDisplayHeight) {
        mX = aX;
        mY = aY;
        mWidth = aWidth;
        mHeight = aHeight;
        mDisplayWidth = aDisplayWidth;
        mDisplayHeight = aDisplayHeight;
    }

    public VideoLayoutConfiguration() {
        mX = INVALID_VALUE;
        mY = INVALID_VALUE;
        mWidth = INVALID_VALUE;
        mHeight = INVALID_VALUE;
        mDisplayWidth = INVALID_VALUE;
        mDisplayHeight = INVALID_VALUE;
    }
}

