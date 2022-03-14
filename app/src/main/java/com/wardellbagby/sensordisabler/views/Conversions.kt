package com.wardellbagby.sensordisabler.views

import android.content.res.Resources

val Int.dp: Int
  get() = (this * Resources.getSystem().displayMetrics.density).toInt()