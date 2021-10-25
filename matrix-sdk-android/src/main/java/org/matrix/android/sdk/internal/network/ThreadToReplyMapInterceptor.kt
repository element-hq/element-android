/*
 * Copyright (c) 2021 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.network

import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import org.matrix.android.sdk.api.extensions.tryOrNull
import org.matrix.android.sdk.api.network.ApiInterceptorListener
import org.matrix.android.sdk.api.network.ApiPath
import org.matrix.android.sdk.internal.di.MatrixScope
import timber.log.Timber
import javax.inject.Inject

/**
 * The goal of this interceptor is to map thread events to be handled as replies.
 * The interceptor is responsible for mapping a thread event:
 *          "m.relates_to":{
 *              "event_id":"$eventId",
 *              "rel_type":"io.element.thread"
 *          }
 * to an equivalent reply event:
 *           m.relates_to":{
 *              "m.in_reply_to":{
 *              "event_id":"$eventId"
 *           }
 */
@MatrixScope
internal class ThreadToReplyMapInterceptor @Inject constructor() : Interceptor {

    init {
        Timber.d("MapThreadToReplyInterceptor.init")
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        val response = chain.proceed(request)

        if (isSyncRequest(request)) {
            Timber.i(" ------> found SYNC REQUEST")

            return response.body?.let {
                val contentType = it.contentType()
                val rawBody = it.string()
                Timber.i(" ------> $rawBody")

                if(rawBody.contains("\"rel_type\":\"io.element.thread\"")){
                    Timber.i(" ------> Thread found")
                    val start = rawBody.indexOf("\"rel_type\":\"io.element.thread\"") - "\"m.relates_to\":{\"event_id\":\"-GoMTnxkfmZczOPvbjcK43WqNib3wiJVaeO_vRxwHIDA\",\"".length +1
                    val end = rawBody.indexOf("\"rel_type\":\"io.element.thread\"") + "\"rel_type\":\"io.element.thread\"".length +2
                    val substr = rawBody.subSequence(start,end)
                    val newRaw = rawBody.replaceRange(start,end,"\"m.relates_to\":{\"m.in_reply_to\":{\"event_id\":\"\$HDddlX2bJQmVS0bN5R9HDzcrGDap18b3cFDDYjTjctc\"}},")
                    Timber.i(" ------> ${substr}")
                    Timber.i(" ------> new raw $newRaw")
                    val newBody = newRaw.toResponseBody(contentType)
                    response.newBuilder().body(newBody).build()

                }else{
                    val newBody = rawBody.toResponseBody(contentType)
                    response.newBuilder().body(newBody).build()
                }
            } ?: response

//            response.peekBody(Long.MAX_VALUE).string().let { networkResponse ->
//                Timber.i(" ------> ThreadToReplyMapInterceptor $networkResponse")
//            }
        }

//        val path = request.url.encodedPath
//        if(path.contains("/sync/")){
//            Timber.i("-----> SYNC REQUEST --> $responseBody")
//
//
//        }

//        val body = ResponseBody.create()
//        val newResponse = response.newBuilder().body(body)
        return response
    }

    /**
     * Returns true if the request is a sync request, false otherwise
     * Example of a sync request:
     * https://matrix-client.matrix.org/_matrix/client/r0/sync?filter=0&set_presence=online&t...
     */
    private fun isSyncRequest(request: Request): Boolean =
            ApiPath.SYNC.method == request.method && request.url.encodedPath.contains(ApiPath.SYNC.path)
}
