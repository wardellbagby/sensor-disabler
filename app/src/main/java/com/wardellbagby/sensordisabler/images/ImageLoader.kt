package com.wardellbagby.sensordisabler.images

import coil.ImageLoader
import com.squareup.workflow1.ui.ViewEnvironmentKey

val CoilImageLoader = object : ViewEnvironmentKey<ImageLoader>(ImageLoader::class) {
  override val default: ImageLoader
    get() = error("No ImageLoader created!")
}