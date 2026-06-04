package com.example.tuner.ui.main

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/** UI tests for [com.example.tuner.ui.main.MainScreen] components. */
class MainScreenTest {

  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  @Before
  fun setup() {
    composeTestRule.setContent { 
      PermissionRequestScreen(onGrantClick = {}) 
    }
  }

  @Test
  fun grantPermissionButton_exists() {
    composeTestRule.onNodeWithText("Grant Permission").assertExists()
  }
}
