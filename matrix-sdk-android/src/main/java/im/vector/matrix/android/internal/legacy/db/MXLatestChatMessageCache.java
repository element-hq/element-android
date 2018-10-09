/* 
 * Copyright 2015 OpenMarket Ltd
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

package im.vector.matrix.android.internal.legacy.db;

import android.content.Context;
import android.text.TextUtils;

import im.vector.matrix.android.internal.legacy.util.ContentUtils;
import im.vector.matrix.android.internal.legacy.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Map;

public class MXLatestChatMessageCache {
    private static final String LOG_TAG = MXLatestChatMessageCache.class.getSimpleName();
    private static final String FILENAME = "ConsoleLatestChatMessageCache";

    final String MXLATESTMESSAGES_STORE_FOLDER = "MXLatestMessagesStore";

    private Map<String, String> mLatestMesssageByRoomId = null;
    private String mUserId = null;
    private File mLatestMessagesDirectory = null;
    private File mLatestMessagesFile = null;

    /**
     * Constructor
     *
     * @param userId the user id
     */
    public MXLatestChatMessageCache(String userId) {
        mUserId = userId;
    }

    /**
     * Clear the text caches.
     *
     * @param context The application context to use.
     */
    public void clearCache(Context context) {
        ContentUtils.deleteDirectory(mLatestMessagesDirectory);
        mLatestMesssageByRoomId = null;
    }

    /**
     * Open the texts cache file.
     *
     * @param context the context.
     */
    private void openLatestMessagesDict(Context context) {

        // already checked
        if (null != mLatestMesssageByRoomId) {
            return;
        }

        mLatestMesssageByRoomId = new HashMap<>();

        try {
            mLatestMessagesDirectory = new File(context.getApplicationContext().getFilesDir(), MXLATESTMESSAGES_STORE_FOLDER);
            mLatestMessagesDirectory = new File(mLatestMessagesDirectory, mUserId);

            mLatestMessagesFile = new File(mLatestMessagesDirectory, FILENAME.hashCode() + "");

            if (!mLatestMessagesDirectory.exists()) {

                // create dir tree
                mLatestMessagesDirectory.mkdirs();

                File oldFile = new File(context.getApplicationContext().getFilesDir(), FILENAME.hashCode() + "");

                // backward compatibility
                if (oldFile.exists()) {
                    oldFile.renameTo(mLatestMessagesFile);
                }
            }

            if (mLatestMessagesFile.exists()) {
                FileInputStream fis = new FileInputStream(mLatestMessagesFile);
                ObjectInputStream ois = new ObjectInputStream(fis);
                mLatestMesssageByRoomId = (Map) ois.readObject();
                ois.close();
                fis.close();
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, "## openLatestMessagesDict failed " + e.getMessage(), e);
        }
    }

    /**
     * Get the latest written text for a dedicated room.
     *
     * @param context the context.
     * @param roomId  the roomId
     * @return the latest message
     */
    public String getLatestText(Context context, String roomId) {
        if (null == mLatestMesssageByRoomId) {
            openLatestMessagesDict(context);
        }

        if (TextUtils.isEmpty(roomId)) {
            return "";
        }

        if (mLatestMesssageByRoomId.containsKey(roomId)) {
            return mLatestMesssageByRoomId.get(roomId);
        }

        return "";
    }

    /**
     * Update the latest message dictionnary.
     *
     * @param context the context.
     */
    private void saveLatestMessagesDict(Context context) {
        try {
            FileOutputStream fos = new FileOutputStream(mLatestMessagesFile);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(mLatestMesssageByRoomId);
            oos.close();
            fos.close();
        } catch (Exception e) {
            Log.e(LOG_TAG, "## saveLatestMessagesDict() failed " + e.getMessage(), e);
        }
    }

    /**
     * Update the latest message for a dedicated roomId.
     *
     * @param context the context.
     * @param roomId  the roomId.
     * @param message the message.
     */
    public void updateLatestMessage(Context context, String roomId, String message) {
        if (null == mLatestMesssageByRoomId) {
            openLatestMessagesDict(context);
        }

        if (TextUtils.isEmpty(message)) {
            mLatestMesssageByRoomId.remove(roomId);
        }

        mLatestMesssageByRoomId.put(roomId, message);
        saveLatestMessagesDict(context);
    }
}
