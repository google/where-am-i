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

import android.accessibilityservice.AccessibilityService
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.accessibility.AccessibilityChecks
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.activityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule.grant
import org.hamcrest.Matchers
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WhereAmIInstrumentedTest {
    @Rule
    @JvmField
    val rule = activityScenarioRule<WhereAmIActivity>()

    @Rule
    @JvmField
    val grantPermissionRule = grant(
        android.Manifest.permission.ACCESS_FINE_LOCATION,
        android.Manifest.permission.WAKE_LOCK,
    )

    @Test
    fun testIncrement() {
        // TODO implement idling
        // https://developer.android.com/training/testing/espresso/idling-resource
        Thread.sleep(5000)

        onView(withId(R.id.text)).check(matches(withText(Matchers.startsWith("You were at"))))
        onView(withId(R.id.text)).check(matches(withContentDescription(Matchers.startsWith("You were at"))))
    }

    companion object {
        @BeforeClass
        @JvmStatic
        fun enableAccessibilityChecks() {
            // TODO why doesn't @android:color/transparent fail tests
            AccessibilityChecks.enable().setRunChecksFromRootView(true)
        }
    }
}