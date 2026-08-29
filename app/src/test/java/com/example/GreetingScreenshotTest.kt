package com.example

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.data.model.BrushType
import com.example.data.model.StrokeModifier
import com.example.ui.components.BrushPaletteBar
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun greeting_screenshot() {
    composeTestRule.setContent {
      MyApplicationTheme {
        BrushPaletteBar(
          selectedBrush = BrushType.NEON,
          selectedModifier = StrokeModifier.WAVE,
          strokeWidth = 14f,
          strokeAlpha = 1.0f,
          currentColor = Color(0xFF00E5FF),
          isPremiumUnlocked = true,
          onSelectBrush = {},
          onSelectModifier = {},
          onSelectStrokeWidth = {},
          onSelectStrokeAlpha = {},
          onOpenStickers = {},
          onClearCanvas = {},
          onUndo = {},
          onRedo = {},
          onOpenPaywall = {},
          onMinimize = {}
        )
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
  }
}


