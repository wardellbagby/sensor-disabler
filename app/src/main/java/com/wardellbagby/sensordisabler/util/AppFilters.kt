package com.wardellbagby.sensordisabler.util

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import com.wardellbagby.sensordisabler.util.Constants.PREFS_FILE_NAME
import com.wardellbagby.sensordisabler.util.Constants.PREFS_KEY_BLOCKLIST

enum class FilterType {
  None, Allow, Deny
}

fun Context.getFilterType(): FilterType {
  val prefs = getSharedPreferences(PREFS_FILE_NAME, MODE_PRIVATE)
  return prefs.getFilterType()
}

fun SharedPreferences.getFilterType(): FilterType {
  return getString(PREFS_KEY_BLOCKLIST, null)
    .let {
      when (it) {
        FilterType.Allow.name -> FilterType.Allow
        FilterType.Deny.name -> FilterType.Deny
        else -> FilterType.None
      }
    }
}

fun Context.setFilterType(filterType: FilterType) {
  val prefs = getSharedPreferences(PREFS_FILE_NAME, MODE_PRIVATE)
  prefs.edit()
    .putString(PREFS_KEY_BLOCKLIST, filterType.name)
    .apply()
}