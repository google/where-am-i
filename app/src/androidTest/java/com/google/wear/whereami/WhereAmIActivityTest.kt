/*
 * Copyright 2020 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.wear.whereami;

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.google.wear.whereami.data.LocationResult
import org.junit.Rule
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class WhereAmIActivityTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    val now = LocalDateTime.of(2021, 8, 18, 12, 31)
        .atZone(ZoneId.systemDefault()).toInstant()

    @Test
    fun testLocationResultDisplay() {
        composeTestRule.setContent {
            LocationResultDisplay(
                location = LocationResult(locationName = "Kings Cross", time = now),
                refreshFn = {})
        }

        composeTestRule.onNodeWithTag("Location")
            .assertTextEquals("Kings Cross")
            .assertContentDescriptionEquals("Kings Cross")
            .assertIsDisplayed()

        composeTestRule.onNodeWithTag("Time")
            .assertTextEquals("12:31 PM")
            .assertContentDescriptionEquals("Updated at 12:31 PM")
            .assertIsDisplayed()

        composeTestRule.onNodeWithText("Refresh")
            .assertTextEquals("Refresh")
            .assertContentDescriptionEquals("Refresh Location")
            .assertHasClickAction()
            .assertIsDisplayed()
    }
} 