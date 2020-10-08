/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.matrix.android.sdk.internal.di

import javax.inject.Qualifier

/**
 * Used to inject the userId
 */
@Qualifier
@Retention(AnnotationRetention.RUNTIME)
internal annotation class UserId

/**
 * Used to inject the deviceId
 */
@Qualifier
@Retention(AnnotationRetention.RUNTIME)
internal annotation class DeviceId

/**
 * Used to inject the md5 of the userId
 */
@Qualifier
@Retention(AnnotationRetention.RUNTIME)
internal annotation class UserMd5

/**
 * Used to inject the sessionId, which is defined as md5(userId|deviceId)
 */
@Qualifier
@Retention(AnnotationRetention.RUNTIME)
internal annotation class SessionId
