package com.wardellbagby.sensordisabler.settings.filtering.settings

import android.widget.ProgressBar
import com.squareup.workflow1.ui.AndroidViewRendering
import com.squareup.workflow1.ui.BuilderViewFactory
import com.squareup.workflow1.ui.backPressedHandler
import com.squareup.workflow1.ui.bindShowRendering

data class LoadingAppsScreen(
  val onBack: () -> Unit
) : AndroidViewRendering<LoadingAppsScreen> {
  override val viewFactory = BuilderViewFactory(
    type = LoadingAppsScreen::class,
    viewConstructor = { initialRendering, initialViewEnvironment, contextForNewView, _ ->
      val view = ProgressBar(contextForNewView).apply {
        isIndeterminate = true
        fun update(rendering: LoadingAppsScreen) {
          backPressedHandler = rendering.onBack
        }

        bindShowRendering(initialRendering, initialViewEnvironment) { rendering, _ ->
          update(rendering)
        }
      }
      return@BuilderViewFactory view
    })
}