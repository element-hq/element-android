/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.test.fixtures

import org.matrix.android.sdk.api.auth.registration.Stage

fun aDummyStage() = Stage.Dummy(mandatory = true)
fun anEmailStage() = Stage.Email(mandatory = true)
fun aMsisdnStage() = Stage.Msisdn(mandatory = true)
fun aTermsStage() = Stage.Terms(mandatory = true, policies = emptyMap<String, String>())
fun aRecaptchaStage() = Stage.ReCaptcha(mandatory = true, publicKey = "any-key")
fun anOtherStage() = Stage.Other(mandatory = true, type = "raw-type", params = emptyMap<String, String>())
