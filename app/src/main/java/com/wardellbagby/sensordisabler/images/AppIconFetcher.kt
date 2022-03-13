package com.wardellbagby.sensordisabler.images

import android.content.pm.PackageManager
import androidx.appcompat.content.res.AppCompatResources
import coil.decode.DataSource
import coil.fetch.DrawableResult
import coil.fetch.Fetcher

data class AppIconRequest(val packageName: String)

val appIconFetcherFactory = Fetcher.Factory { data: AppIconRequest, options, _ ->
  Fetcher {
    try {
      DrawableResult(
        drawable = options.context.packageManager.getApplicationIcon(data.packageName),
        isSampled = false,
        dataSource = DataSource.DISK
      )
    } catch (exception: PackageManager.NameNotFoundException) {
      DrawableResult(
        drawable = AppCompatResources.getDrawable(options.context, android.R.color.transparent)!!,
        isSampled = false,
        dataSource = DataSource.DISK
      )
    }
  }
}