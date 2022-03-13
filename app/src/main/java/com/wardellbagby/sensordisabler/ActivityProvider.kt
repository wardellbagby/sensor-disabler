package com.wardellbagby.sensordisabler

import android.app.Activity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ActivityProvider
@Inject constructor() : DefaultLifecycleObserver {
  var activity: Activity? = null

  override fun onCreate(owner: LifecycleOwner) {
    activity = owner as Activity
  }

  override fun onDestroy(owner: LifecycleOwner) {
    activity = null
  }
}