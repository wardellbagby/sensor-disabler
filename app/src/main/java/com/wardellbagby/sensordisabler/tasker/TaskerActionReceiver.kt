package com.wardellbagby.sensordisabler.tasker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.wardellbagby.sensordisabler.util.ProUtil
import com.wardellbagby.sensordisabler.util.saveSettings

class TaskerActionReceiver : BroadcastReceiver() {

  override fun onReceive(context: Context, intent: Intent) {

    if (taskerFireSettings != intent.action) {
      Log.w(
        "Sensor Disabler: Tasker",
        "Not firing Tasker change for Sensor Disabler as intent action is invalid: ${intent.action}"
      )
      return
    }

    if (!ProUtil.isPro(context)) {
      Log.w(
        "Sensor Disabler: Tasker",
        "Not firing Tasker change for Sensor Disabler as Pro is not enabled."
      )
      return
    }

    val bundle = intent.getBundleExtra(extraBundleKey) ?: return

    val sensor = bundle.getSensor(context) ?: return
    val modificationType = bundle.getModificationType(sensor) ?: return

    sensor.saveSettings(context, modificationType)
  }
}