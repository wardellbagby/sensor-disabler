package com.wardellbagby.sensordisabler.xposed

import android.util.Log
import com.wardellbagby.sensordisabler.BuildConfig
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

/**
 * Sets the return value of [XposedAvailable.isXposedAvailable] to be true.
 */
fun XC_LoadPackage.LoadPackageParam.setXposedVisibilityForManager() {
  if (processName == BuildConfig.APPLICATION_ID) {
    Log.e("Sensor Disabler", "DISABLING for $processName")
    XposedHelpers.findAndHookMethod(
      "com.wardellbagby.sensordisabler.xposed.XposedAvailable",
      classLoader,
      "isXposedAvailable",
      methodHook(
        before = {
          Log.e("Sensor Disabler", "hooked Xposed ")
        },
        after = {
          result = true
        })
    )
  }
}