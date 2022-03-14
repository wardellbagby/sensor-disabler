package com.wardellbagby.sensordisabler.settings

import com.google.android.material.progressindicator.CircularProgressIndicator
import com.squareup.workflow1.ui.AndroidViewRendering
import com.squareup.workflow1.ui.BuilderViewFactory
import com.squareup.workflow1.ui.bindShowRendering

object LoadingScreen : AndroidViewRendering<LoadingScreen> {
  override val viewFactory = BuilderViewFactory(
    type = LoadingScreen::class
  ) { initialRendering, initialViewEnvironment, contextForNewView, _ ->
    CircularProgressIndicator(contextForNewView).apply {
      isIndeterminate = true
      bindShowRendering(
        initialRendering = initialRendering,
        initialViewEnvironment = initialViewEnvironment,
        showRendering = { _, _ -> })
    }
  }

}