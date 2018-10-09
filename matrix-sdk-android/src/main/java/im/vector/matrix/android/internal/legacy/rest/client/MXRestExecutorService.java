/*
 * Copyright 2016 OpenMarket Ltd
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

package im.vector.matrix.android.internal.legacy.rest.client;

import android.os.HandlerThread;
import android.support.annotation.NonNull;

import im.vector.matrix.android.internal.legacy.util.MXOsHandler;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * MXRestExecutor is a basic thread executor
 */
public class MXRestExecutorService extends AbstractExecutorService {
    private HandlerThread mHandlerThread;
    private MXOsHandler mHandler;

    public MXRestExecutorService() {
        mHandlerThread = new HandlerThread("MXRestExecutor" + hashCode(), Thread.MIN_PRIORITY);
        mHandlerThread.start();
        mHandler = new MXOsHandler(mHandlerThread.getLooper());
    }

    @Override
    public void execute(final Runnable r) {
        mHandler.post(r);
    }

    /**
     * Stop any running thread
     */
    public void stop() {
        if (null != mHandlerThread) {
            mHandlerThread.quit();
        }
    }

    @Override public void shutdown() {

    }

    @NonNull @Override public List<Runnable> shutdownNow() {
        return Collections.emptyList();
    }

    @Override public boolean isShutdown() {
        return false;
    }

    @Override public boolean isTerminated() {
        return false;
    }

    @Override
    public boolean awaitTermination(long timeout, @NonNull TimeUnit unit) throws InterruptedException {
        return false;
    }
}
