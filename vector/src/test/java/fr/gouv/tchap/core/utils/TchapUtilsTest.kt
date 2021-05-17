/*
 * Copyright (c) 2021 New Vector Ltd
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

package fr.gouv.tchap.core.utils

import junit.framework.TestCase
import org.junit.Assert
import org.junit.Test

class TchapUtilsTest : TestCase() {

    /**
     * Test getting full name without domain
     */

    @Test
    fun `given a displayName if it contains brackets then return its first element after split`() {
        // Given
        val displayNameTest = "Nom Prenom [Modernisation]"

        // When
        val result = TchapUtils.getNameFromDisplayName(displayNameTest)

        // Then
        Assert.assertEquals(result, "Nom Prenom")
    }

    @Test
    fun `given a displayName if it doesn't contain brackets then return the original display name`() {
        // Given
        val displayNameTest = "Nom Prenom"

        // When
        val result = TchapUtils.getNameFromDisplayName(displayNameTest)

        // Then
        Assert.assertEquals(result, "Nom Prenom")
    }

    /**
     * Test getting domain only
     */

    @Test
    fun `given a displayName if it contains brackets then return domain name inside`() {
        // Given
        val displayNameTest = "Nom Prenom [Modernisation]"

        // When
        val result = TchapUtils.getDomainFromDisplayName(displayNameTest)

        // Then
        Assert.assertEquals(result, "Modernisation")
    }

    @Test
    fun `given a displayName if it doesn't contain brackets then return empty string`() {
        // Given
        val displayNameTest = "Nom Prenom"

        // When
        val result = TchapUtils.getDomainFromDisplayName(displayNameTest)

        // Then
        Assert.assertEquals(result, "")
    }
}
