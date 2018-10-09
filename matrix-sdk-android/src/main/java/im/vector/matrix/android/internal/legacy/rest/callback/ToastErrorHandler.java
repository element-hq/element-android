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
package im.vector.matrix.android.internal.legacy.rest.callback;

import android.content.Context;
import android.widget.Toast;

import im.vector.matrix.android.internal.legacy.rest.model.MatrixError;

/**
 * Failure callback that shows different toast messages.
 */
public class ToastErrorHandler implements ApiFailureCallback {

    private final Context context;
    private final String msgPrefix;

    /**
     * Constructor with context for the toast messages and a common prefix for messages.
     *
     * @param context   the context - needed for toast
     * @param msgPrefix the message prefix
     */
    public ToastErrorHandler(Context context, String msgPrefix) {
        this.context = context;
        this.msgPrefix = msgPrefix;
    }

    @Override
    public void onNetworkError(Exception e) {
        Toast.makeText(context, appendPrefix("Connection error"), Toast.LENGTH_LONG).show();
    }

    @Override
    public void onMatrixError(MatrixError e) {
        Toast.makeText(context, appendPrefix(e.getLocalizedMessage()), Toast.LENGTH_LONG).show();
    }

    @Override
    public void onUnexpectedError(Exception e) {
        Toast.makeText(context, appendPrefix(null), Toast.LENGTH_LONG).show();
    }

    String appendPrefix(String text) {
        return (text == null) ? msgPrefix : msgPrefix + ": " + text;
    }
}
