/*
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
package im.vector.matrix.android.internal.legacy.rest.api;

import im.vector.matrix.android.internal.legacy.rest.model.EncryptedMediaScanBody;
import im.vector.matrix.android.internal.legacy.rest.model.EncryptedMediaScanEncryptedBody;
import im.vector.matrix.android.internal.legacy.rest.model.MediaScanPublicKeyResult;
import im.vector.matrix.android.internal.legacy.rest.model.MediaScanResult;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

/**
 * The matrix content scanner REST API.
 */
public interface MediaScanApi {
    /**
     * Get the current public curve25519 key that the AV server is advertising.
     */
    @GET("public_key")
    Call<MediaScanPublicKeyResult> getServerPublicKey();

    /**
     * Scan an unencrypted file.
     *
     * @param domain  the server name
     * @param mediaId the user id
     */
    @GET("scan/{domain}/{mediaId}")
    Call<MediaScanResult> scanUnencrypted(@Path("domain") String domain, @Path("mediaId") String mediaId);

    /**
     * Scan an encrypted file.
     *
     * @param encryptedMediaScanBody the encryption information required to decrypt the content before scanning it.
     */
    @POST("scan_encrypted")
    Call<MediaScanResult> scanEncrypted(@Body EncryptedMediaScanBody encryptedMediaScanBody);

    /**
     * Scan an encrypted file, sending an encrypted body.
     *
     * @param encryptedMediaScanEncryptedBody the encrypted encryption information required to decrypt the content before scanning it.
     */
    @POST("scan_encrypted")
    Call<MediaScanResult> scanEncrypted(@Body EncryptedMediaScanEncryptedBody encryptedMediaScanEncryptedBody);
}
