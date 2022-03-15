package com.wardellbagby.sensordisabler

import com.crossbowffs.remotepreferences.RemotePreferenceProvider
import com.wardellbagby.sensordisabler.util.Constants

class SensorDisablerPreferenceProvider : RemotePreferenceProvider(
  "com.wardellbagby.sensordisabler", arrayOf(
    Constants.PREFS_FILE_NAME
  )
) {
  override fun checkAccess(prefFileName: String, prefKey: String, write: Boolean): Boolean {
    // Allow all reads, but block all writes. Only Sensor Disabler should be able to write.
    return !write
  }
}